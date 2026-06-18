package bm.b0b0b0.soulDrone.listener;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.drone.DeliveryDrone;
import bm.b0b0b0.soulDrone.drone.DroneManager;
import bm.b0b0b0.soulDrone.gui.DeliveryCargoMenu;
import bm.b0b0b0.soulDrone.gui.DeliveryPickupMenu;
import bm.b0b0b0.soulDrone.gui.DeliveryPreviewMenu;
import bm.b0b0b0.soulDrone.gui.StoredPickupMenu;
import bm.b0b0b0.soulDrone.service.DeliveryService;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class DeliveryListener implements Listener {

    private final PluginConfig config;
    private final DeliveryService deliveryService;
    private final DroneManager droneManager;

    public DeliveryListener(
            PluginConfig config,
            DeliveryService deliveryService,
            DroneManager droneManager
    ) {
        this.config = config;
        this.deliveryService = deliveryService;
        this.droneManager = droneManager;
    }

    @EventHandler
    public void onDroneClick(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        handleDroneInteraction(event.getPlayer(), event.getRightClicked().getUniqueId(), event);
    }

    @EventHandler
    public void onDroneClickEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        handleDroneInteraction(event.getPlayer(), event.getRightClicked().getUniqueId(), event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        DeliveryDrone drone = droneManager.findNearestInteractable(player, config.clickRadius());
        if (drone == null) {
            return;
        }

        event.setCancelled(true);
        deliveryService.handleDroneClick(player, drone);
    }

    private void handleDroneInteraction(Player player, UUID entityId, Cancellable event) {
        DeliveryDrone drone = droneManager.byEntity(entityId);
        if (drone == null) {
            return;
        }
        event.setCancelled(true);
        if (!drone.isInteractable()) {
            return;
        }
        deliveryService.handleDroneClick(player, drone);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDronePunchSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (deliveryService.hasOpenDroneMenu(player)) {
            return;
        }
        DeliveryDrone drone = droneManager.findNearestPunchable(player, config.dronePunchReach());
        if (drone == null) {
            return;
        }
        deliveryService.handleDronePunch(player, drone);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDroneDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        DeliveryDrone drone = droneManager.byEntity(event.getEntity().getUniqueId());
        if (drone == null || !drone.isPunchable()) {
            return;
        }
        if (deliveryService.hasOpenDroneMenu(player)) {
            return;
        }
        event.setCancelled(true);
        deliveryService.handleDronePunch(player, drone);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder(false) instanceof DeliveryPreviewMenu) {
            event.setCancelled(true);
            return;
        }
        if (event.getInventory().getHolder(false) instanceof StoredPickupMenu storedMenu) {
            handlePickupClick(event, storedMenu.ownerId(), storedMenu);
            return;
        }
        if (event.getInventory().getHolder(false) instanceof DeliveryPickupMenu pickupMenu) {
            handlePickupClick(event, pickupMenu.receiverId(), pickupMenu);
            return;
        }
        if (!(event.getInventory().getHolder(false) instanceof DeliveryCargoMenu menu)) {
            return;
        }
        handleSenderCargoClick(event, menu);
    }

    private void handlePickupClick(InventoryClickEvent event, UUID allowedPlayerId, InventoryHolder menu) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(allowedPlayerId)) {
            event.setCancelled(true);
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            event.setCancelled(true);
            return;
        }

        if (clickedInventory.getHolder(false) == menu) {
            if (menu instanceof DeliveryPickupMenu pickupMenu && !pickupMenu.isCargoSlot(event.getSlot())) {
                event.setCancelled(true);
                return;
            }
            if (menu instanceof StoredPickupMenu storedMenu && !storedMenu.isCargoSlot(event.getSlot())) {
                event.setCancelled(true);
                return;
            }
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.isEmpty()) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
        }
    }

    private void handleSenderCargoClick(InventoryClickEvent event, DeliveryCargoMenu menu) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(menu.senderId())) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory().getHolder(false) instanceof DeliveryCargoMenu) {
            if (!menu.isCargoSlot(event.getSlot())) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction().name().contains("MOVE_TO_OTHER_INVENTORY")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof DeliveryPreviewMenu) {
            event.setCancelled(true);
            return;
        }
        if (event.getInventory().getHolder(false) instanceof StoredPickupMenu menu) {
            for (int slot : event.getRawSlots()) {
                if (slot < menu.getInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        if (event.getInventory().getHolder(false) instanceof DeliveryPickupMenu menu) {
            for (int slot : event.getRawSlots()) {
                if (slot < menu.getInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        if (!(event.getInventory().getHolder(false) instanceof DeliveryCargoMenu menu)) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < menu.getInventory().getSize() && !menu.isCargoSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder(false) instanceof StoredPickupMenu storedMenu) {
            deliveryService.onStoredMenuClosed(player, storedMenu);
            return;
        }
        if (event.getInventory().getHolder(false) instanceof DeliveryPickupMenu pickupMenu) {
            deliveryService.onPickupMenuClosed(player, pickupMenu);
            return;
        }
        if (event.getInventory().getHolder(false) instanceof DeliveryCargoMenu menu) {
            deliveryService.onCargoMenuClosed(player, menu);
        }
    }

}
