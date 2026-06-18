package bm.b0b0b0.soulDrone.drone;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DeliveryDrone {

    private final UUID id = UUID.randomUUID();
    private final PluginConfig config;
    private final MessageService messages;
    private final UUID senderId;
    private final UUID receiverId;
    private final String senderName;
    private final String receiverName;

    private DeliveryPhase phase = DeliveryPhase.ASSEMBLING;
    private DroneVisual visual;
    private World world;
    private Location anchor;
    private Location landingTarget;
    private final float[] aimAngles = new float[2];
    private final Matrix4f[] assemblyStartMatrices;
    private Matrix4f[] arrivalStartMatrices;

    private int phaseTick;
    private int phaseLimit;
    private int hoverAge;
    private float departureSpin;
    private boolean notifiedReceiver;
    private boolean cargoLoaded;
    private final List<ItemStack> cargo = new ArrayList<>();

    private Runnable onSenderTimeout;
    private Runnable onReceiverTimeout;
    private Runnable onDelivered;

    public DeliveryDrone(
            PluginConfig config,
            MessageService messages,
            Player sender,
            Player receiver,
            Location spawnAnchor,
            float yaw
    ) {
        this.config = config;
        this.messages = messages;
        this.senderId = sender.getUniqueId();
        this.receiverId = receiver.getUniqueId();
        this.senderName = sender.getName();
        this.receiverName = receiver.getName();
        this.world = spawnAnchor.getWorld();
        this.anchor = spawnAnchor.clone();
        this.aimAngles[0] = yaw;
        this.aimAngles[1] = 0f;

        visual = new DroneVisual(config, messages, world, anchor, receiverName, senderName);
        assemblyStartMatrices = visual.createAssemblyStartMatrices();
        phaseLimit = config.assemblyDurationTicks();
        phaseTick = 0;
    }

    public UUID id() {
        return id;
    }

    public UUID senderId() {
        return senderId;
    }

    public UUID receiverId() {
        return receiverId;
    }

    public String senderName() {
        return senderName;
    }

    public String receiverName() {
        return receiverName;
    }

    public DeliveryPhase phase() {
        return phase;
    }

    public List<UUID> entityIds() {
        return visual != null ? visual.entityIds() : List.of();
    }

    public Location anchorLocation() {
        return anchor != null ? anchor.clone() : null;
    }

    public boolean isInteractable() {
        return isWaitingSender() || isWaitingReceiver();
    }

    public boolean isWaitingSender() {
        return phase == DeliveryPhase.WAITING_SENDER;
    }

    public boolean isWaitingReceiver() {
        return phase == DeliveryPhase.WAITING_RECEIVER;
    }

    public void setOnSenderTimeout(Runnable onSenderTimeout) {
        this.onSenderTimeout = onSenderTimeout;
    }

    public void setOnReceiverTimeout(Runnable onReceiverTimeout) {
        this.onReceiverTimeout = onReceiverTimeout;
    }

    public void setOnDelivered(Runnable onDelivered) {
        this.onDelivered = onDelivered;
    }

    public void loadCargo(List<ItemStack> items) {
        cargo.clear();
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) {
                cargo.add(item.clone());
            }
        }
        cargoLoaded = !cargo.isEmpty();
    }

    public List<ItemStack> cargo() {
        List<ItemStack> copy = new ArrayList<>(cargo.size());
        for (ItemStack item : cargo) {
            copy.add(item.clone());
        }
        return copy;
    }

    public boolean hasCargo() {
        return cargoLoaded && !cargo.isEmpty();
    }

    public void beginDeparture() {
        if (phase != DeliveryPhase.WAITING_SENDER || !cargoLoaded) {
            return;
        }
        switchPhase(DeliveryPhase.DEPARTING);
    }

    public void beginPickup(Player receiver) {
        if (phase != DeliveryPhase.WAITING_RECEIVER) {
            return;
        }
        giveCargo(receiver);
        if (onDelivered != null) {
            onDelivered.run();
        }
        finish(false);
    }

    public boolean tick() {
        return switch (phase) {
            case ASSEMBLING -> tickAssembly();
            case WAITING_SENDER -> tickWaitingSender();
            case DEPARTING -> tickDeparting();
            case IN_TRANSIT -> tickInTransit();
            case ARRIVING -> tickArriving();
            case WAITING_RECEIVER -> tickWaitingReceiver();
            case DONE -> false;
        };
    }

    public void forceRemove() {
        finish(false);
    }

    private boolean tickAssembly() {
        phaseTick++;
        float progress = Math.min(1f, phaseTick / (float) Math.max(1, phaseLimit));
        float eased = easeOutCubic(progress);
        visual.updateAssembly(anchor, aimAngles[0], aimAngles[1], eased, phaseTick, assemblyStartMatrices);

        if (progress >= 1f) {
            world.playSound(anchor, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
            world.spawnParticle(Particle.WITCH, anchor, 12, 0.4, 0.4, 0.4, 0.02);
            switchPhase(DeliveryPhase.WAITING_SENDER);
        }
        return true;
    }

    private boolean tickWaitingSender() {
        hoverAge++;
        applyHover();
        visual.update(anchor, aimAngles[0], aimAngles[1]);

        if (phaseTick >= phaseLimit) {
            if (onSenderTimeout != null) {
                onSenderTimeout.run();
            }
            finish(true);
            return false;
        }
        return true;
    }

    private boolean tickDeparting() {
        phaseTick++;
        departureSpin += (float) Math.toRadians(config.departureSpinSpeed());
        anchor.add(0, config.departureRiseSpeed(), 0);
        visual.update(anchor, aimAngles[0], aimAngles[1], departureSpin);

        if (phaseTick % 4 == 0) {
            world.spawnParticle(Particle.PORTAL, anchor, 3, 0.2, 0.2, 0.2, 0.01);
        }

        if (phaseTick >= phaseLimit) {
            world.playSound(anchor, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.3f);
            world.spawnParticle(Particle.REVERSE_PORTAL, anchor, 20, 0.5, 0.5, 0.5, 0.05);
            visual.remove();
            visual = null;
            switchPhase(DeliveryPhase.IN_TRANSIT);
        }
        return true;
    }

    private boolean tickInTransit() {
        Player receiver = Bukkit.getPlayer(receiverId);
        if (receiver == null || !receiver.isOnline()) {
            phaseTick++;
            if (phaseTick > 20 * 60 * 5) {
                returnCargoToSender();
                finish(true);
                return false;
            }
            return true;
        }

        world = receiver.getWorld();
        landingTarget = landingSpot(receiver);
        anchor = landingTarget.clone().add(0, config.arrivalHeight(), 0);
        aimAngles[0] = receiver.getLocation().getYaw();
        aimAngles[1] = 15f;

        visual = new DroneVisual(config, messages, world, anchor, receiverName, senderName);
        arrivalStartMatrices = visual.createAssemblyStartMatrices();
        notifiedReceiver = false;
        switchPhase(DeliveryPhase.ARRIVING);
        return true;
    }

    private boolean tickArriving() {
        phaseTick++;
        float progress = Math.min(1f, phaseTick / (float) Math.max(1, phaseLimit));
        float eased = easeOutCubic(progress);

        if (landingTarget != null) {
            double startY = landingTarget.getY() + config.arrivalHeight();
            double endY = landingTarget.getY();
            anchor.setY(startY + (endY - startY) * eased);
        }

        visual.updateAssembly(anchor, aimAngles[0], aimAngles[1], eased, phaseTick, arrivalStartMatrices);

        if (!notifiedReceiver) {
            notifyReceiver();
            notifiedReceiver = true;
        }

        if (progress >= 1f) {
            world.playSound(anchor, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.9f);
            world.spawnParticle(Particle.WITCH, anchor, 10, 0.3, 0.3, 0.3, 0.02);
            switchPhase(DeliveryPhase.WAITING_RECEIVER);
        }
        return true;
    }

    private boolean tickWaitingReceiver() {
        hoverAge++;
        applyHover();
        visual.update(anchor, aimAngles[0], aimAngles[1]);

        if (phaseTick >= phaseLimit) {
            if (onReceiverTimeout != null) {
                onReceiverTimeout.run();
            }
            returnCargoToSender();
            finish(true);
            return false;
        }
        return true;
    }

    private void applyHover() {
        Vector lateral = DroneVisual.lateralFromYawPitch(aimAngles[0], aimAngles[1]);
        double bob = Math.sin(hoverAge * config.bobSpeed()) * config.bobAmplitude();
        double sway = Math.sin(hoverAge * config.swaySpeed()) * config.swayAmplitude();
        anchor.add(0, bob, 0);
        anchor.add(lateral.clone().multiply(sway));
    }

    private void switchPhase(DeliveryPhase next) {
        phase = next;
        phaseTick = 0;
        hoverAge = 0;
        departureSpin = 0f;
        phaseLimit = switch (next) {
            case ASSEMBLING -> config.assemblyDurationTicks();
            case WAITING_SENDER -> config.senderWaitTicks();
            case DEPARTING -> config.departureDurationTicks();
            case IN_TRANSIT -> 1;
            case ARRIVING -> config.arrivalDurationTicks();
            case WAITING_RECEIVER -> config.receiverWaitTicks();
            case DONE -> 0;
        };
    }

    private void notifyReceiver() {
        Player receiver = Bukkit.getPlayer(receiverId);
        if (receiver == null || !receiver.isOnline()) {
            return;
        }
        receiver.sendMessage(messages.component("package-arriving-chat", senderName));
        receiver.showTitle(net.kyori.adventure.title.Title.title(
                messages.component("package-arriving-title"),
                messages.component("package-arriving-subtitle", senderName),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(250),
                        java.time.Duration.ofMillis(3500),
                        java.time.Duration.ofMillis(750)
                )
        ));
    }

    private void giveCargo(Player receiver) {
        for (ItemStack item : cargo) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            var leftover = receiver.getInventory().addItem(item.clone());
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    receiver.getWorld().dropItemNaturally(receiver.getLocation(), drop);
                }
            }
        }
        cargo.clear();
    }

    private void returnCargoToSender() {
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            if (landingTarget != null && world != null) {
                for (ItemStack item : cargo) {
                    if (item != null && !item.isEmpty()) {
                        world.dropItemNaturally(landingTarget, item.clone());
                    }
                }
            }
            cargo.clear();
            return;
        }
        for (ItemStack item : cargo) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            var leftover = sender.getInventory().addItem(item.clone());
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    sender.getWorld().dropItemNaturally(sender.getLocation(), drop);
                }
            }
        }
        cargo.clear();
    }

    private void finish(boolean withEffect) {
        if (withEffect && anchor != null && world != null) {
            world.spawnParticle(Particle.CLOUD, anchor, 10, 0.4, 0.4, 0.4, 0.02);
            world.spawnParticle(Particle.BLOCK, anchor, 12, 0.4, 0.4, 0.4, 0.04, Material.PURPLE_CONCRETE.createBlockData());
            world.playSound(anchor, Sound.ENTITY_BEE_LOOP, 0.3f, 0.6f);
        }
        if (visual != null) {
            visual.remove();
            visual = null;
        }
        phase = DeliveryPhase.DONE;
    }

    private Location landingSpot(Player receiver) {
        Location base = receiver.getLocation().clone();
        Vector direction = base.getDirection().setY(0);
        if (direction.lengthSquared() < 0.0001) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();
        return base.add(direction.multiply(config.landingDistance())).add(0, 0.2, 0);
    }

    private static float easeOutCubic(float progress) {
        return 1f - (1f - progress) * (1f - progress) * (1f - progress);
    }

}
