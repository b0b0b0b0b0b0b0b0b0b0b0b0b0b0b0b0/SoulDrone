package bm.b0b0b0.soulDrone.service;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.gui.StoredPickupMenu;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.model.StoredPackage;
import bm.b0b0b0.soulDrone.model.StoredPackageKind;
import bm.b0b0b0.soulDrone.repository.PackageRepository;
import bm.b0b0b0.soulDrone.util.CargoLayout;
import bm.b0b0b0.soulDrone.util.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.UUID;

public final class StoredPackageService {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final MessageService messages;
    private final PackageRepository repository;

    private DeliveryService deliveryService;
    private final Map<UUID, UUID> openMenus = new HashMap<>();

    public StoredPackageService(
            JavaPlugin plugin,
            PluginConfig config,
            MessageService messages,
            PackageRepository repository
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.repository = repository;
    }

    public void setDeliveryService(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    public void deletePackage(UUID packageId) {
        repository.delete(packageId);
    }

    public void storeForReceiver(
            UUID senderId,
            String senderName,
            UUID receiverId,
            String receiverName,
            Map<Integer, ItemStack> items
    ) {
        if (items == null || items.isEmpty()) {
            return;
        }
        insertPackage(StoredPackageKind.TO_RECEIVER, senderId, senderName, receiverId, receiverName, items);
    }

    public void storeForSender(
            UUID senderId,
            String senderName,
            UUID receiverId,
            String receiverName,
            Map<Integer, ItemStack> items
    ) {
        if (items == null || items.isEmpty()) {
            return;
        }
        insertPackage(StoredPackageKind.TO_SENDER, senderId, senderName, receiverId, receiverName, items);
    }

    public void handlePlayerJoin(Player player) {
        repository.deleteExpired(System.currentTimeMillis()).thenCompose(ignored ->
                repository.findForPlayer(player.getUniqueId())
        ).thenAccept(packages -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            long now = System.currentTimeMillis();
            for (StoredPackage storedPackage : packages) {
                if (storedPackage.expiresAt() <= now) {
                    continue;
                }
                if (!isOwner(player, storedPackage)) {
                    continue;
                }
                notifyStoredPackage(player, storedPackage);
            }
        }));
    }

    public void claim(Player player, UUID packageId) {
        repository.findById(packageId).thenAccept(optional -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (optional.isEmpty()) {
                player.sendMessage(messages.component("stored-not-found"));
                return;
            }
            StoredPackage storedPackage = optional.get();
            if (storedPackage.expiresAt() <= System.currentTimeMillis()) {
                repository.delete(storedPackage.id());
                player.sendMessage(messages.component("stored-expired"));
                return;
            }
            if (!isOwner(player, storedPackage)) {
                player.sendMessage(messages.component("stored-not-yours"));
                return;
            }
            if (deliveryService != null && deliveryService.startStoredDelivery(player, storedPackage)) {
                return;
            }
            openMenu(player, storedPackage);
        }));
    }

    public void claimFirst(Player player) {
        repository.findForPlayer(player.getUniqueId()).thenAccept(packages -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            long now = System.currentTimeMillis();
            for (StoredPackage storedPackage : packages) {
                if (storedPackage.expiresAt() <= now) {
                    continue;
                }
                if (!isOwner(player, storedPackage)) {
                    continue;
                }
                if (deliveryService != null && deliveryService.startStoredDelivery(player, storedPackage)) {
                    return;
                }
                openMenu(player, storedPackage);
                return;
            }
            player.sendMessage(messages.component("stored-no-pending"));
        }));
    }

    public void refuse(Player player, UUID packageId) {
        if (packageId == null) {
            player.sendMessage(messages.component("stored-refuse-usage"));
            return;
        }
        repository.findById(packageId).thenAccept(optional -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (optional.isEmpty()) {
                player.sendMessage(messages.component("stored-not-found"));
                return;
            }
            StoredPackage storedPackage = optional.get();
            if (!isOwner(player, storedPackage)) {
                player.sendMessage(messages.component("stored-not-yours"));
                return;
            }
            refusePackage(player, storedPackage);
        }));
    }

    public void onMenuClosed(Player player, StoredPickupMenu menu) {
        openMenus.remove(player.getUniqueId());
        repository.findById(menu.packageId()).thenAccept(optional -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (optional.isEmpty()) {
                giveItems(player, menu.collectRemainingCargoBySlot());
                return;
            }
            StoredPackage storedPackage = optional.get();
            Map<Integer, ItemStack> remaining = menu.collectRemainingCargoBySlot();
            if (!CargoLayout.hasItems(remaining)) {
                repository.delete(storedPackage.id());
                if (storedPackage.kind() == StoredPackageKind.TO_RECEIVER) {
                    player.sendMessage(messages.component("package-received", storedPackage.senderName()));
                } else {
                    player.sendMessage(messages.component("stored-return-claimed", storedPackage.receiverName()));
                }
                return;
            }
            try {
                repository.updateItems(storedPackage.id(), ItemStackSerializer.serializeCargo(remaining));
            } catch (Exception exception) {
                giveItems(player, remaining);
                plugin.getLogger().warning("Failed to update stored package items: " + exception.getMessage());
                return;
            }
            player.sendMessage(messages.component("stored-partial-kept"));
        }));
    }

    private void refusePackage(Player player, StoredPackage storedPackage) {
        if (storedPackage.kind() == StoredPackageKind.TO_RECEIVER) {
            repository.updateKind(
                    storedPackage.id(),
                    StoredPackageKind.TO_SENDER,
                    storedPackage.senderId(),
                    storedPackage.senderName(),
                    storedPackage.receiverId(),
                    storedPackage.receiverName()
            );
            player.sendMessage(messages.component("stored-refused-receiver", storedPackage.senderName()));
            Player sender = Bukkit.getPlayer(storedPackage.senderId());
            if (sender != null && sender.isOnline()) {
                String claimCommand = "/send " + config.claimSubcommand() + " " + storedPackage.id();
                String refuseCommand = "/send " + config.refuseSubcommand() + " " + storedPackage.id();
                sender.sendMessage(messages.component(
                        "stored-notify-sender",
                        storedPackage.receiverName(),
                        claimCommand,
                        refuseCommand
                ));
            }
            return;
        }
        repository.delete(storedPackage.id());
        player.sendMessage(messages.component("stored-refused-sender"));
    }

    private void openMenu(Player player, StoredPackage storedPackage) {
        if (openMenus.containsKey(player.getUniqueId())) {
            return;
        }
        StoredPickupMenu menu = new StoredPickupMenu(
                config,
                messages,
                storedPackage.id(),
                player.getUniqueId(),
                storedPackage.kind(),
                storedPackage.kind() == StoredPackageKind.TO_RECEIVER
                        ? storedPackage.senderName()
                        : storedPackage.receiverName(),
                storedPackage.cargo()
        );
        openMenus.put(player.getUniqueId(), storedPackage.id());
        player.openInventory(menu.getInventory());
    }

    private void notifyStoredPackage(Player player, StoredPackage storedPackage) {
        String claimCommand = "/send " + config.claimSubcommand() + " " + storedPackage.id();
        String refuseCommand = "/send " + config.refuseSubcommand() + " " + storedPackage.id();
        if (storedPackage.kind() == StoredPackageKind.TO_RECEIVER) {
            player.sendMessage(messages.component(
                    "stored-notify-receiver",
                    storedPackage.senderName(),
                    claimCommand,
                    refuseCommand
            ));
            return;
        }
        player.sendMessage(messages.component(
                "stored-notify-sender",
                storedPackage.receiverName(),
                claimCommand,
                refuseCommand
        ));
    }

    private void insertPackage(
            StoredPackageKind kind,
            UUID senderId,
            String senderName,
            UUID receiverId,
            String receiverName,
            Map<Integer, ItemStack> cargo
    ) {
        long now = System.currentTimeMillis();
        Map<Integer, ItemStack> copy = CargoLayout.clone(cargo);
        if (!CargoLayout.hasItems(copy)) {
            return;
        }
        StoredPackage storedPackage = new StoredPackage(
                UUID.randomUUID(),
                kind,
                senderId,
                senderName,
                receiverId,
                receiverName,
                copy,
                now,
                now + config.packageStorageMillis()
        );
        repository.insert(storedPackage);
    }

    private boolean isOwner(Player player, StoredPackage storedPackage) {
        UUID playerId = player.getUniqueId();
        if (storedPackage.kind() == StoredPackageKind.TO_RECEIVER) {
            return playerId.equals(storedPackage.receiverId());
        }
        return playerId.equals(storedPackage.senderId());
    }

    private void giveItems(Player player, Map<Integer, ItemStack> items) {
        for (ItemStack item : items.values()) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            var leftover = player.getInventory().addItem(item.clone());
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }

}
