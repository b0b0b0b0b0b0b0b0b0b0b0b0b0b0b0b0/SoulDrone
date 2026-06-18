package bm.b0b0b0.soulDrone.listener;

import bm.b0b0b0.soulDrone.service.DeliveryService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {

    private final DeliveryService deliveryService;

    public PlayerJoinListener(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        deliveryService.handlePlayerJoin(event.getPlayer());
    }

}
