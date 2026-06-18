package bm.b0b0b0.soulDrone.listener;

import bm.b0b0b0.soulDrone.service.DeliveryService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {

    private final DeliveryService deliveryService;

    public PlayerQuitListener(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        deliveryService.handlePlayerQuit(event.getPlayer());
    }

}
