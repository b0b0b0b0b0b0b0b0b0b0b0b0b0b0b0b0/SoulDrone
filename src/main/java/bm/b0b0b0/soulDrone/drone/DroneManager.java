package bm.b0b0b0.soulDrone.drone;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DroneManager {

    private final List<DeliveryDrone> activeDrones = new ArrayList<>();
    private final Map<UUID, DeliveryDrone> entityIndex = new HashMap<>();
    private BukkitRunnable tickTask;

    public void start(JavaPlugin plugin) {
        if (tickTask != null) {
            return;
        }
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickAll();
            }
        };
        tickTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void register(DeliveryDrone drone) {
        activeDrones.add(drone);
        indexDrone(drone);
    }

    public DeliveryDrone byEntity(UUID entityId) {
        return entityIndex.get(entityId);
    }

    public DeliveryDrone findNearestInteractable(Player player, double maxDistance) {
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();
        DeliveryDrone nearest = null;
        double nearestDistance = maxDistance;

        for (DeliveryDrone drone : activeDrones) {
            if (!drone.isInteractable()) {
                continue;
            }
            Location anchor = drone.anchorLocation();
            if (anchor == null || anchor.getWorld() == null || !anchor.getWorld().equals(player.getWorld())) {
                continue;
            }
            double distance = anchor.distance(player.getLocation());
            if (distance > maxDistance) {
                continue;
            }
            Vector toDrone = anchor.toVector().subtract(eye.toVector());
            if (toDrone.lengthSquared() < 0.0001) {
                toDrone = new Vector(0, 0, 1);
            } else {
                toDrone.normalize();
            }
            if (look.dot(toDrone) < 0.35) {
                continue;
            }
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = drone;
            }
        }
        return nearest;
    }

    public DeliveryDrone findNearestPunchable(Player player, double maxDistance) {
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();
        DeliveryDrone nearest = null;
        double nearestDistance = maxDistance;

        for (DeliveryDrone drone : activeDrones) {
            if (!drone.isPunchable()) {
                continue;
            }
            Location anchor = drone.anchorLocation();
            if (anchor == null || anchor.getWorld() == null || !anchor.getWorld().equals(player.getWorld())) {
                continue;
            }
            double distance = anchor.distance(player.getLocation());
            if (distance > maxDistance) {
                continue;
            }
            Vector toDrone = anchor.toVector().subtract(eye.toVector());
            if (toDrone.lengthSquared() < 0.0001) {
                toDrone = new Vector(0, 0, 1);
            } else {
                toDrone.normalize();
            }
            if (look.dot(toDrone) < 0.45) {
                continue;
            }
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = drone;
            }
        }
        return nearest;
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (DeliveryDrone drone : activeDrones) {
            drone.forceRemove();
        }
        activeDrones.clear();
        entityIndex.clear();
    }

    public void unregister(DeliveryDrone drone) {
        activeDrones.remove(drone);
        for (UUID entityId : drone.entityIds()) {
            entityIndex.remove(entityId);
        }
    }

    private void tickAll() {
        Iterator<DeliveryDrone> iterator = activeDrones.iterator();
        while (iterator.hasNext()) {
            DeliveryDrone drone = iterator.next();
            reindexIfNeeded(drone);
            if (!drone.tick()) {
                iterator.remove();
                for (UUID entityId : drone.entityIds()) {
                    entityIndex.remove(entityId);
                }
            }
        }
    }

    private void indexDrone(DeliveryDrone drone) {
        for (UUID entityId : drone.entityIds()) {
            entityIndex.put(entityId, drone);
        }
    }

    private void reindexIfNeeded(DeliveryDrone drone) {
        for (UUID entityId : drone.entityIds()) {
            if (!entityIndex.containsKey(entityId)) {
                entityIndex.put(entityId, drone);
            }
        }
    }

}
