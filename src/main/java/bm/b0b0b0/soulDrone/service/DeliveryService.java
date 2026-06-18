package bm.b0b0b0.soulDrone.service;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.data.ReceiverToggleStore;
import bm.b0b0b0.soulDrone.drone.DeliveryDrone;
import bm.b0b0b0.soulDrone.drone.DeliveryPhase;
import bm.b0b0b0.soulDrone.drone.DroneManager;
import bm.b0b0b0.soulDrone.economy.VaultEconomyService;
import bm.b0b0b0.soulDrone.gui.DeliveryCargoMenu;
import bm.b0b0b0.soulDrone.gui.DeliveryPickupMenu;
import bm.b0b0b0.soulDrone.gui.DeliveryPreviewMenu;
import bm.b0b0b0.soulDrone.gui.StoredPickupMenu;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.model.DeliveryRequest;
import bm.b0b0b0.soulDrone.model.StoredPackage;
import bm.b0b0b0.soulDrone.model.StoredPackageKind;
import bm.b0b0b0.soulDrone.util.CargoLayout;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeliveryService {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final MessageService messages;
    private final VaultEconomyService economy;
    private final DroneManager droneManager;
    private final ReceiverToggleStore receiverToggleStore;
    private StoredPackageService storedPackageService;

    private final Map<UUID, DeliveryDrone> senderDrones = new HashMap<>();
    private final Map<UUID, DeliveryDrone> openMenuDrones = new HashMap<>();
    private final Map<UUID, DeliveryDrone> openPickupMenus = new HashMap<>();
    private final Map<UUID, DeliveryRequest> pendingBySender = new HashMap<>();
    private final Map<UUID, Map<UUID, DeliveryRequest>> pendingByReceiver = new HashMap<>();
    private final Map<UUID, BukkitTask> requestTimeoutTasks = new HashMap<>();
    private final Map<UUID, Long> dronePunchCooldownUntilTick = new HashMap<>();

    public DeliveryService(
            JavaPlugin plugin,
            PluginConfig config,
            MessageService messages,
            VaultEconomyService economy,
            DroneManager droneManager,
            ReceiverToggleStore receiverToggleStore
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.economy = economy;
        this.droneManager = droneManager;
        this.receiverToggleStore = receiverToggleStore;
    }

    public void setStoredPackageService(StoredPackageService storedPackageService) {
        this.storedPackageService = storedPackageService;
    }

    public boolean isStorageReady() {
        return storedPackageService != null;
    }

    public void claimStored(Player player, String packageIdRaw) {
        if (!isStorageReady()) {
            player.sendMessage(messages.component("storage-not-ready"));
            return;
        }
        if (packageIdRaw == null || packageIdRaw.isBlank()) {
            storedPackageService.claimFirst(player);
            return;
        }
        try {
            storedPackageService.claim(player, UUID.fromString(packageIdRaw));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(messages.component("stored-not-found"));
        }
    }

    public void refuseStored(Player player, String packageIdRaw) {
        if (!isStorageReady()) {
            player.sendMessage(messages.component("storage-not-ready"));
            return;
        }
        if (packageIdRaw == null || packageIdRaw.isBlank()) {
            player.sendMessage(messages.component("stored-refuse-usage"));
            return;
        }
        try {
            storedPackageService.refuse(player, UUID.fromString(packageIdRaw));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(messages.component("stored-not-found"));
        }
    }

    public void onStoredMenuClosed(Player player, bm.b0b0b0.soulDrone.gui.StoredPickupMenu menu) {
        if (storedPackageService != null) {
            storedPackageService.onMenuClosed(player, menu);
        }
    }

    public void handlePlayerJoin(Player player) {
        if (storedPackageService != null) {
            storedPackageService.handlePlayerJoin(player);
        }
    }

    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();

        DeliveryRequest pending = pendingBySender.remove(playerId);
        if (pending != null) {
            removeRequest(pending);
        }

        DeliveryDrone drone = senderDrones.get(playerId);
        if (drone == null) {
            return;
        }

        DeliveryPhase phase = drone.phase();
        if (phase != DeliveryPhase.ASSEMBLING && phase != DeliveryPhase.WAITING_SENDER) {
            return;
        }

        closeOpenMenu(playerId);
        drone.forceRemove();
    }

    public boolean toggleReceiving(Player player) {
        if (!player.hasPermission(config.togglePermission())) {
            player.sendMessage(messages.component("no-toggle-permission"));
            return false;
        }

        boolean accepts = receiverToggleStore.toggle(player.getUniqueId());
        if (accepts) {
            player.sendMessage(messages.component("toggle-enabled"));
        } else {
            player.sendMessage(messages.component("toggle-disabled"));
        }
        return true;
    }

    public boolean acceptsDeliveries(UUID playerId) {
        return receiverToggleStore.acceptsDeliveries(playerId);
    }

    public boolean initiateSend(Player sender, Player receiver) {
        return initiateSend(sender, receiver.getUniqueId(), receiver.getName(), true);
    }

    public boolean initiateSend(Player sender, UUID targetId, String targetName, boolean targetOnline) {
        if (!receiverToggleStore.acceptsDeliveries(targetId)) {
            sender.sendMessage(messages.component("target-rejects-drones", targetName));
            return false;
        }

        if (senderDrones.containsKey(sender.getUniqueId())) {
            purgeStaleSenderDrone(sender.getUniqueId());
        }
        if (senderDrones.containsKey(sender.getUniqueId())) {
            sender.sendMessage(messages.component("already-has-drone"));
            return false;
        }
        if (pendingBySender.containsKey(sender.getUniqueId())) {
            sender.sendMessage(messages.component("already-has-request"));
            return false;
        }

        if (targetOnline && config.requireReceiverAccept()) {
            Player receiver = Bukkit.getPlayer(targetId);
            if (receiver != null) {
                return createRequest(sender, receiver);
            }
        }

        if (!targetOnline && config.requireReceiverAccept() && !config.autoAcceptOffline()) {
            sender.sendMessage(messages.component("target-needs-accept-online", targetName));
            return false;
        }

        return startSend(sender, targetId, targetName);
    }

    public boolean acceptRequest(Player receiver, String senderName) {
        if (!receiver.hasPermission(config.acceptPermission())) {
            receiver.sendMessage(messages.component("no-accept-permission"));
            return false;
        }

        DeliveryRequest request = resolveRequest(receiver, senderName, true);
        if (request == null) {
            return false;
        }

        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender == null || !sender.isOnline()) {
            receiver.sendMessage(messages.component("request-sender-offline"));
            removeRequest(request);
            return false;
        }

        if (senderDrones.containsKey(sender.getUniqueId())) {
            purgeStaleSenderDrone(sender.getUniqueId());
        }
        if (senderDrones.containsKey(sender.getUniqueId())) {
            receiver.sendMessage(messages.component("request-sender-busy"));
            removeRequest(request);
            return false;
        }

        if (!canAffordSend(sender)) {
            receiver.sendMessage(messages.component("request-sender-no-funds"));
            sender.sendMessage(messages.component("insufficient-funds", economy.format(config.sendCost())));
            removeRequest(request);
            return false;
        }

        removeRequest(request);
        receiver.sendMessage(messages.component("request-accepted-receiver", sender.getName()));
        sender.sendMessage(messages.component("request-accepted-sender", receiver.getName()));
        return startSend(sender, request.receiverId(), request.receiverName());
    }

    public boolean denyRequest(Player receiver, String senderName) {
        if (!receiver.hasPermission(config.denyPermission())) {
            receiver.sendMessage(messages.component("no-deny-permission"));
            return false;
        }

        DeliveryRequest request = resolveRequest(receiver, senderName, true);
        if (request == null) {
            return false;
        }

        removeRequest(request);
        receiver.sendMessage(messages.component("request-denied-receiver", request.senderName()));

        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(messages.component("request-denied-sender", receiver.getName()));
        }
        return true;
    }

    public boolean startSend(Player sender, Player receiver) {
        return startSend(sender, receiver.getUniqueId(), receiver.getName());
    }

    public boolean startSend(Player sender, UUID receiverId, String receiverName) {
        if (senderDrones.containsKey(sender.getUniqueId())) {
            purgeStaleSenderDrone(sender.getUniqueId());
        }
        if (senderDrones.containsKey(sender.getUniqueId())) {
            sender.sendMessage(messages.component("already-has-drone"));
            return false;
        }

        if (!canAffordSend(sender)) {
            return false;
        }

        Location spawn = spawnLocation(sender);
        DeliveryDrone drone = new DeliveryDrone(
                config,
                messages,
                sender.getUniqueId(),
                sender.getName(),
                receiverId,
                receiverName,
                spawn,
                sender.getLocation().getYaw()
        );

        wireStoreCallbacks(drone, sender.getUniqueId(), sender.getName(), receiverId, receiverName);

        drone.setOnFinished(() -> cleanupSender(sender.getUniqueId(), drone.id()));

        drone.setOnSenderTimeout(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            closeOpenMenu(sender.getUniqueId());
            Player onlineSender = Bukkit.getPlayer(sender.getUniqueId());
            if (onlineSender != null) {
                onlineSender.sendMessage(messages.component("drone-sender-timeout"));
            }
        }));

        drone.setOnReceiverTimeout(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            closeOpenPickupMenu(receiverId);
            Player onlineReceiver = Bukkit.getPlayer(receiverId);
            if (onlineReceiver != null) {
                onlineReceiver.sendMessage(messages.component("drone-receiver-timeout"));
            }
        }));

        senderDrones.put(sender.getUniqueId(), drone);
        droneManager.register(drone);
        sender.sendMessage(messages.component("drone-spawned"));
        Player targetOnline = Bukkit.getPlayer(receiverId);
        if (targetOnline == null || !targetOnline.isOnline()) {
            sender.sendMessage(messages.component("offline-target-notice", receiverName));
        }
        return true;
    }

    public boolean startStoredDelivery(Player recipient, StoredPackage storedPackage) {
        if (senderDrones.containsKey(storedPackage.senderId())) {
            purgeStaleSenderDrone(storedPackage.senderId());
        }
        if (senderDrones.containsKey(storedPackage.senderId())) {
            recipient.sendMessage(messages.component("already-has-drone"));
            return false;
        }

        boolean returnToSender = storedPackage.kind() == StoredPackageKind.TO_SENDER;
        Location spawn = spawnLocation(recipient);
        DeliveryDrone drone = new DeliveryDrone(
                config,
                messages,
                storedPackage.senderId(),
                storedPackage.senderName(),
                storedPackage.receiverId(),
                storedPackage.receiverName(),
                spawn,
                recipient.getLocation().getYaw()
        );
        drone.loadCargo(storedPackage.cargo());
        drone.setLocalDelivery(true);
        drone.setReturnDelivery(returnToSender);
        drone.setStoredDelivery(true);
        drone.setStoredPackageId(storedPackage.id());

        wireStoreCallbacks(
                drone,
                storedPackage.senderId(),
                storedPackage.senderName(),
                storedPackage.receiverId(),
                storedPackage.receiverName()
        );
        drone.setOnFinished(() -> cleanupSender(storedPackage.senderId(), drone.id()));

        senderDrones.put(storedPackage.senderId(), drone);
        droneManager.register(drone);
        finalizeStoredPackage(drone);

        if (returnToSender) {
            recipient.sendMessage(messages.component("package-return-arriving", storedPackage.receiverName()));
        } else {
            recipient.sendMessage(messages.component("package-arriving-chat", storedPackage.senderName()));
        }
        return true;
    }

    private void wireStoreCallbacks(
            DeliveryDrone drone,
            UUID senderId,
            String senderName,
            UUID receiverId,
            String receiverName
    ) {
        if (storedPackageService == null) {
            return;
        }
        drone.setOnStoreForReceiver(() -> storedPackageService.storeForReceiver(
                senderId,
                senderName,
                receiverId,
                receiverName,
                drone.cargoBySlot()
        ));
        drone.setOnStoreForSender(() -> storedPackageService.storeForSender(
                senderId,
                senderName,
                receiverId,
                receiverName,
                drone.cargoBySlot()
        ));
    }

    private void finalizeStoredPackage(DeliveryDrone drone) {
        if (storedPackageService == null || drone.storedPackageId() == null) {
            return;
        }
        UUID packageId = drone.storedPackageId();
        storedPackageService.deletePackage(packageId);
        drone.setStoredPackageId(null);
    }

    private boolean createRequest(Player sender, Player receiver) {
        DeliveryRequest request = new DeliveryRequest(
                sender.getUniqueId(),
                sender.getName(),
                receiver.getUniqueId(),
                receiver.getName()
        );

        pendingBySender.put(sender.getUniqueId(), request);
        pendingByReceiver
                .computeIfAbsent(receiver.getUniqueId(), ignored -> new HashMap<>())
                .put(sender.getUniqueId(), request);

        scheduleRequestTimeout(request);

        sender.sendMessage(messages.component("request-sent", receiver.getName()));
        String acceptCommand = "/send " + config.acceptSubcommand() + " " + sender.getName();
        String denyCommand = "/send " + config.denySubcommand() + " " + sender.getName();
        receiver.sendMessage(messages.component(
                "request-received",
                sender.getName(),
                acceptCommand,
                denyCommand
        ));
        return true;
    }

    private DeliveryRequest resolveRequest(Player receiver, String senderName, boolean notify) {
        Map<UUID, DeliveryRequest> requests = pendingByReceiver.get(receiver.getUniqueId());
        if (requests == null || requests.isEmpty()) {
            if (notify) {
                receiver.sendMessage(messages.component("request-no-pending"));
            }
            return null;
        }

        if (senderName == null || senderName.isBlank()) {
            if (requests.size() == 1) {
                return requests.values().iterator().next();
            }
            if (notify) {
                receiver.sendMessage(messages.component("request-specify-sender"));
            }
            return null;
        }

        for (DeliveryRequest request : requests.values()) {
            if (request.senderName().equalsIgnoreCase(senderName)) {
                return request;
            }
        }

        if (notify) {
            receiver.sendMessage(messages.component("request-not-found", senderName));
        }
        return null;
    }

    private void scheduleRequestTimeout(DeliveryRequest request) {
        BukkitTask previous = requestTimeoutTasks.remove(request.requestId());
        if (previous != null) {
            previous.cancel();
        }

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!pendingBySender.containsKey(request.senderId())) {
                return;
            }
            DeliveryRequest current = pendingBySender.get(request.senderId());
            if (current == null || !current.requestId().equals(request.requestId())) {
                return;
            }
            removeRequest(request);

            Player sender = Bukkit.getPlayer(request.senderId());
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(messages.component("request-expired-sender", request.receiverName()));
            }
            Player receiver = Bukkit.getPlayer(request.receiverId());
            if (receiver != null && receiver.isOnline()) {
                receiver.sendMessage(messages.component("request-expired-receiver", request.senderName()));
            }
        }, config.requestWaitTicks());

        requestTimeoutTasks.put(request.requestId(), task);
    }

    private void removeRequest(DeliveryRequest request) {
        pendingBySender.remove(request.senderId());

        Map<UUID, DeliveryRequest> receiverRequests = pendingByReceiver.get(request.receiverId());
        if (receiverRequests != null) {
            receiverRequests.remove(request.senderId());
            if (receiverRequests.isEmpty()) {
                pendingByReceiver.remove(request.receiverId());
            }
        }

        BukkitTask task = requestTimeoutTasks.remove(request.requestId());
        if (task != null) {
            task.cancel();
        }
    }

    private boolean canAffordSend(Player sender) {
        double cost = config.sendCost();
        if (cost <= 0.0 || sender.hasPermission(config.bypassCostPermission())) {
            return true;
        }
        if (config.requireVault() && !economy.isAvailable()) {
            sender.sendMessage(messages.component("vault-unavailable"));
            return false;
        }
        if (economy.isAvailable() && !economy.has(sender, cost)) {
            sender.sendMessage(messages.component("insufficient-funds", economy.format(cost)));
            return false;
        }
        return true;
    }

    public void handleDroneClick(Player player, DeliveryDrone drone) {
        if (!drone.isInteractable()) {
            return;
        }

        if (config.cargoPreviewOnSneak() && player.isSneaking()) {
            if (!drone.canManageCargo(player.getUniqueId())) {
                player.sendMessage(messages.component("drone-not-yours"));
                return;
            }
            openCargoPreview(player, drone);
            return;
        }

        if (!drone.canManageCargo(player.getUniqueId())) {
            player.sendMessage(messages.component("drone-not-yours"));
            return;
        }

        if (drone.isWaitingSender()) {
            if (!player.hasPermission(config.openPermission())) {
                player.sendMessage(messages.component("no-open-permission"));
                return;
            }
            if (drone.isReturnDelivery()) {
                if (!player.hasPermission(config.receivePermission())) {
                    player.sendMessage(messages.component("no-receive-permission"));
                    return;
                }
                openPickupMenu(player, drone);
                return;
            }
            openCargoMenu(player, drone);
            return;
        }

        if (drone.isWaitingReceiver()) {
            if (!player.hasPermission(config.receivePermission())) {
                player.sendMessage(messages.component("no-receive-permission"));
                return;
            }
            openPickupMenu(player, drone);
        }
    }

    public boolean hasOpenDroneMenu(Player player) {
        if (openMenuDrones.containsKey(player.getUniqueId())) {
            return true;
        }
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder(false);
        return holder instanceof DeliveryCargoMenu
                || holder instanceof DeliveryPickupMenu
                || holder instanceof DeliveryPreviewMenu
                || holder instanceof StoredPickupMenu;
    }

    public void handleDronePunch(Player attacker, DeliveryDrone drone) {
        if (!config.dronePunchEnabled() || !drone.isPunchable()) {
            return;
        }
        if (hasOpenDroneMenu(attacker)) {
            return;
        }

        long nowTick = plugin.getServer().getCurrentTick();
        Long cooldownUntil = dronePunchCooldownUntilTick.get(attacker.getUniqueId());
        if (cooldownUntil != null && nowTick < cooldownUntil) {
            return;
        }
        dronePunchCooldownUntilTick.put(
                attacker.getUniqueId(),
                nowTick + config.dronePunchCooldownTicks()
        );

        net.kyori.adventure.text.Component line = messages.randomDronePunchLine();
        Location anchor = drone.anchorLocation();
        if (anchor != null && anchor.getWorld() != null) {
            double radiusSquared = config.dronePunchBroadcastRadius() * config.dronePunchBroadcastRadius();
            for (Player nearby : anchor.getWorld().getPlayers()) {
                if (nearby.getLocation().distanceSquared(anchor) <= radiusSquared) {
                    nearby.sendMessage(line);
                }
            }
        } else {
            attacker.sendMessage(line);
        }

        double damage = config.dronePunchDamage();
        if (damage > 0.0) {
            attacker.damage(damage);
        }

        double knockback = config.dronePunchKnockback();
        if (knockback > 0.0) {
            Location from = anchor != null ? anchor : attacker.getLocation();
            Vector push = attacker.getLocation().toVector().subtract(from.toVector());
            if (push.lengthSquared() < 0.0001) {
                push = attacker.getLocation().getDirection().multiply(-1);
            }
            push.normalize().multiply(knockback);
            push.setY(Math.max(0.18, push.getY() + 0.22));
            attacker.setVelocity(push);
        }
    }

    public void openCargoPreview(Player player, DeliveryDrone drone) {
        if (drone.isWaitingSender()) {
            if (!player.hasPermission(config.openPermission())) {
                player.sendMessage(messages.component("no-open-permission"));
                return;
            }
            openPreviewMenu(
                    player,
                    messages.component("gui-preview-title", drone.receiverName()),
                    drone.cargoBySlot()
            );
            return;
        }

        if (drone.isWaitingReceiver()) {
            if (!player.hasPermission(config.receivePermission())) {
                player.sendMessage(messages.component("no-receive-permission"));
                return;
            }
            if (!drone.hasCargo()) {
                player.sendMessage(messages.component("cargo-preview-empty"));
                return;
            }
            openPreviewMenu(
                    player,
                    messages.component("gui-preview-pickup-title", drone.senderName()),
                    drone.cargoBySlot()
            );
        }
    }

    private void openPreviewMenu(Player player, Component title, Map<Integer, ItemStack> cargo) {
        if (!CargoLayout.hasItems(cargo)) {
            player.sendMessage(messages.component("cargo-preview-empty"));
        }
        DeliveryPreviewMenu menu = new DeliveryPreviewMenu(config, title, cargo);
        player.openInventory(menu.getInventory());
    }

    public void openPickupMenu(Player picker, DeliveryDrone drone) {
        boolean returnAtSender = drone.isReturnDelivery() && drone.isWaitingSender();
        if (returnAtSender) {
            if (!picker.getUniqueId().equals(drone.senderId())) {
                return;
            }
        } else if (!picker.getUniqueId().equals(drone.receiverId()) || !drone.isWaitingReceiver()) {
            return;
        }
        if (openPickupMenus.containsKey(picker.getUniqueId())) {
            return;
        }
        if (!drone.hasCargo()) {
            return;
        }

        Map<Integer, ItemStack> cargo = drone.takeCargoForMenuBySlot();
        drone.setReceiverMenuOpen(true);

        DeliveryPickupMenu menu = new DeliveryPickupMenu(
                config,
                messages,
                drone.id(),
                picker.getUniqueId(),
                drone.senderId(),
                drone.senderName(),
                cargo
        );
        openPickupMenus.put(picker.getUniqueId(), drone);
        picker.openInventory(menu.getInventory());
    }

    public void onPickupMenuClosed(Player picker, DeliveryPickupMenu menu) {
        openPickupMenus.remove(picker.getUniqueId());
        DeliveryDrone drone = findDrone(menu.droneId());
        Map<Integer, ItemStack> remaining = menu.collectRemainingCargoBySlot();

        if (drone == null) {
            giveItemsToPlayer(picker, remaining);
            return;
        }

        boolean returnAtSender = drone.isReturnDelivery() && drone.isWaitingSender();
        if (!returnAtSender && !drone.isWaitingReceiver()) {
            giveItemsToPlayer(picker, remaining);
            return;
        }

        drone.setReceiverMenuOpen(false);
        drone.restoreCargo(remaining);

        if (!drone.hasCargo()) {
            if (returnAtSender) {
                if (drone.isStoredDelivery()) {
                    picker.sendMessage(messages.component("stored-return-claimed", drone.receiverName()));
                } else {
                    picker.sendMessage(messages.component("package-returned-sender", drone.receiverName()));
                }
            } else {
                picker.sendMessage(messages.component("package-received", drone.senderName()));
            }
            drone.completeDelivery();
            return;
        }

        drone.resetPartialPickupWait();
        picker.sendMessage(messages.component(
                "package-partial-taken",
                (int) Math.round(config.partialPickupWaitSeconds())
        ));
    }

    private DeliveryDrone findDrone(UUID droneId) {
        for (DeliveryDrone drone : senderDrones.values()) {
            if (drone.id().equals(droneId)) {
                return drone;
            }
        }
        return null;
    }

    private void giveItemsToPlayer(Player player, Map<Integer, ItemStack> items) {
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

    public void openCargoMenu(Player sender, DeliveryDrone drone) {
        if (!sender.getUniqueId().equals(drone.senderId()) || !drone.isWaitingSender()) {
            return;
        }
        if (openMenuDrones.containsKey(sender.getUniqueId())) {
            return;
        }

        drone.setSenderMenuOpen(true);
        DeliveryCargoMenu menu = new DeliveryCargoMenu(
                config,
                messages,
                drone.id(),
                sender,
                drone.receiverName()
        );
        openMenuDrones.put(sender.getUniqueId(), drone);
        sender.openInventory(menu.getInventory());
    }

    public void onCargoMenuClosed(Player sender, DeliveryCargoMenu menu) {
        openMenuDrones.remove(sender.getUniqueId());
        DeliveryDrone drone = senderDrones.get(sender.getUniqueId());
        if (drone == null || !drone.id().equals(menu.droneId())) {
            menu.returnCargoToPlayer(sender);
            return;
        }

        drone.setSenderMenuOpen(false);

        if (!menu.hasCargo()) {
            menu.returnCargoToPlayer(sender);
            sender.sendMessage(messages.component("drone-cargo-empty-closed"));
            return;
        }

        double cost = config.sendCost();
        if (cost > 0.0 && !sender.hasPermission(config.bypassCostPermission())) {
            if (config.requireVault() && !economy.isAvailable()) {
                sender.sendMessage(messages.component("vault-unavailable"));
                menu.returnCargoToPlayer(sender);
                return;
            }
            if (economy.isAvailable()) {
                if (!economy.has(sender, cost)) {
                    sender.sendMessage(messages.component("insufficient-funds", economy.format(cost)));
                    menu.returnCargoToPlayer(sender);
                    return;
                }
                if (!economy.withdraw(sender, cost)) {
                    sender.sendMessage(messages.component("vault-unavailable"));
                    menu.returnCargoToPlayer(sender);
                    return;
                }
                sender.sendMessage(messages.component("cost-charged", economy.format(cost)));
            }
        }

        drone.loadCargo(menu.extractCargoBySlot());
        drone.beginDeparture();
        sender.sendMessage(messages.component("package-sent", drone.receiverName()));
    }

    public DeliveryDrone droneForSender(UUID senderId) {
        return senderDrones.get(senderId);
    }

    private Location spawnLocation(Player sender) {
        Location base = sender.getLocation().clone();
        Vector direction = base.getDirection().setY(0);
        if (direction.lengthSquared() < 0.0001) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();
        Location spawn = base.add(direction.multiply(config.spawnDistance()));
        spawn.setY(spawn.getY() + sender.getHeight() + config.spawnHeight());
        return spawn;
    }

    private void purgeStaleSenderDrone(UUID senderId) {
        DeliveryDrone drone = senderDrones.get(senderId);
        if (drone != null && drone.phase() == DeliveryPhase.DONE) {
            senderDrones.remove(senderId);
        }
    }

    private void cleanupSender(UUID senderId, UUID droneId) {
        DeliveryDrone drone = senderDrones.get(senderId);
        if (drone != null && drone.id().equals(droneId)) {
            senderDrones.remove(senderId);
        }
        closeOpenMenu(senderId);
    }

    private void closeOpenPickupMenu(UUID receiverId) {
        DeliveryDrone drone = openPickupMenus.remove(receiverId);
        Player player = Bukkit.getPlayer(receiverId);
        if (player == null) {
            if (drone != null) {
                drone.setReceiverMenuOpen(false);
            }
            return;
        }
        if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof DeliveryPickupMenu menu) {
            Map<Integer, ItemStack> remaining = menu.collectRemainingCargoBySlot();
            player.closeInventory();
            if (drone != null && drone.isWaitingReceiver()) {
                drone.setReceiverMenuOpen(false);
                drone.restoreCargo(remaining);
            } else {
                giveItemsToPlayer(player, remaining);
            }
        } else if (drone != null) {
            drone.setReceiverMenuOpen(false);
        }
    }

    private void closeOpenMenu(UUID senderId) {
        DeliveryDrone openDrone = openMenuDrones.remove(senderId);
        if (openDrone != null) {
            openDrone.setSenderMenuOpen(false);
        }
        Player player = Bukkit.getPlayer(senderId);
        if (player == null) {
            return;
        }
        if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof DeliveryCargoMenu menu) {
            menu.returnCargoToPlayer(player);
            player.closeInventory();
        }
    }

}
