package bm.b0b0b0.soulDrone.service;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.data.ReceiverToggleStore;
import bm.b0b0b0.soulDrone.drone.DeliveryDrone;
import bm.b0b0b0.soulDrone.drone.DroneManager;
import bm.b0b0b0.soulDrone.economy.VaultEconomyService;
import bm.b0b0b0.soulDrone.gui.DeliveryCargoMenu;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.model.DeliveryRequest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DeliveryService {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final MessageService messages;
    private final VaultEconomyService economy;
    private final DroneManager droneManager;
    private final ReceiverToggleStore receiverToggleStore;

    private final Map<UUID, DeliveryDrone> senderDrones = new HashMap<>();
    private final Map<UUID, DeliveryDrone> openMenuDrones = new HashMap<>();
    private final Map<UUID, DeliveryRequest> pendingBySender = new HashMap<>();
    private final Map<UUID, Map<UUID, DeliveryRequest>> pendingByReceiver = new HashMap<>();
    private final Map<UUID, BukkitTask> requestTimeoutTasks = new HashMap<>();

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
        if (!receiverToggleStore.acceptsDeliveries(receiver.getUniqueId())) {
            sender.sendMessage(messages.component("target-rejects-drones", receiver.getName()));
            return false;
        }

        if (senderDrones.containsKey(sender.getUniqueId())) {
            sender.sendMessage(messages.component("already-has-drone"));
            return false;
        }
        if (pendingBySender.containsKey(sender.getUniqueId())) {
            sender.sendMessage(messages.component("already-has-request"));
            return false;
        }

        if (config.requireReceiverAccept()) {
            return createRequest(sender, receiver);
        }
        return startSend(sender, receiver);
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
        return startSend(sender, receiver);
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
                sender,
                receiver,
                spawn,
                sender.getLocation().getYaw()
        );

        drone.setOnSenderTimeout(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            closeOpenMenu(sender.getUniqueId());
            Player onlineSender = Bukkit.getPlayer(sender.getUniqueId());
            if (onlineSender != null) {
                onlineSender.sendMessage(messages.component("drone-sender-timeout"));
            }
            cleanupSender(sender.getUniqueId(), drone.id());
        }));

        drone.setOnReceiverTimeout(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player onlineReceiver = Bukkit.getPlayer(receiver.getUniqueId());
            if (onlineReceiver != null) {
                onlineReceiver.sendMessage(messages.component("drone-receiver-timeout"));
            }
            Player onlineSender = Bukkit.getPlayer(sender.getUniqueId());
            if (onlineSender != null) {
                onlineSender.sendMessage(messages.component("drone-receiver-timeout"));
            }
            cleanupSender(sender.getUniqueId(), drone.id());
        }));

        drone.setOnDelivered(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player onlineReceiver = Bukkit.getPlayer(receiver.getUniqueId());
            if (onlineReceiver != null) {
                onlineReceiver.sendMessage(messages.component("package-received", drone.senderName()));
            }
            cleanupSender(sender.getUniqueId(), drone.id());
        }));

        senderDrones.put(sender.getUniqueId(), drone);
        droneManager.register(drone);
        sender.sendMessage(messages.component("drone-spawned"));
        return true;
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
        if (drone.isWaitingSender()) {
            if (!player.getUniqueId().equals(drone.senderId())) {
                return;
            }
            if (!player.hasPermission(config.openPermission())) {
                player.sendMessage(messages.component("no-open-permission"));
                return;
            }
            openCargoMenu(player, drone);
            return;
        }

        if (drone.isWaitingReceiver()) {
            if (!player.getUniqueId().equals(drone.receiverId())) {
                return;
            }
            if (!player.hasPermission(config.receivePermission())) {
                player.sendMessage(messages.component("no-receive-permission"));
                return;
            }
            drone.beginPickup(player);
            droneManager.unregister(drone);
        }
    }

    public void openCargoMenu(Player sender, DeliveryDrone drone) {
        if (!sender.getUniqueId().equals(drone.senderId()) || !drone.isWaitingSender()) {
            return;
        }
        if (openMenuDrones.containsKey(sender.getUniqueId())) {
            return;
        }

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

        if (!menu.hasCargo()) {
            sender.sendMessage(messages.component("package-empty"));
            menu.returnCargoToPlayer(sender);
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

        ItemStack[] items = menu.extractCargo();
        drone.loadCargo(Arrays.asList(items));
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
        spawn.setY(spawn.getY() + config.spawnHeight());
        return spawn;
    }

    private void cleanupSender(UUID senderId, UUID droneId) {
        DeliveryDrone drone = senderDrones.get(senderId);
        if (drone != null && drone.id().equals(droneId)) {
            senderDrones.remove(senderId);
        }
        closeOpenMenu(senderId);
    }

    private void closeOpenMenu(UUID senderId) {
        openMenuDrones.remove(senderId);
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
