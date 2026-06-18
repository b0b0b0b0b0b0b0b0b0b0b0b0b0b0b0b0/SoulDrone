package bm.b0b0b0.soulDrone.listener;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.drone.DeliveryDrone;
import bm.b0b0b0.soulDrone.drone.DroneManager;
import bm.b0b0b0.soulDrone.gui.DeliveryCargoMenu;
import bm.b0b0b0.soulDrone.service.DeliveryService;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

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
        deliveryService.handleDroneClick(player, drone);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof DeliveryCargoMenu menu)) {
            return;
        }
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
        if (!(event.getInventory().getHolder(false) instanceof DeliveryCargoMenu menu)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        deliveryService.onCargoMenuClosed(player, menu);
    }

}
