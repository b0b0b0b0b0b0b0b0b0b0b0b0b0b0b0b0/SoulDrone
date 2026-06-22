package bm.b0b0b0.soulDrone.drone;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.util.ConfiguredSound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private Matrix4f[] assemblyStartMatrices;

    private int phaseTick;
    private int phaseLimit;
    private int senderIdleTicks;
    private int hoverAge;
    private int particleTick;
    private float departureSpin;
    private boolean notifiedReceiver;
    private boolean cargoLoaded;
    private boolean senderMenuOpen;
    private boolean receiverMenuOpen;
    private boolean returnDelivery;
    private boolean localDelivery;
    private boolean leaveWithEffect;
    private DepartureTarget departureTarget = DepartureTarget.RECEIVER;
    private UUID storedPackageId;
    private boolean storedDelivery;
    private boolean followYawInitialized;
    private float followYaw;
    private Location hoverBase;
    private final Map<Integer, ItemStack> cargo = new LinkedHashMap<>();

    private Runnable onSenderTimeout;
    private Runnable onReceiverTimeout;
    private Runnable onFinished;
    private Runnable onStoreForReceiver;
    private Runnable onStoreForSender;

    public DeliveryDrone(
            PluginConfig config,
            MessageService messages,
            UUID senderId,
            String senderName,
            UUID receiverId,
            String receiverName,
            Location spawnAnchor,
            float yaw
    ) {
        this.config = config;
        this.messages = messages;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.world = spawnAnchor.getWorld();
        this.anchor = spawnAnchor.clone();
        this.hoverBase = spawnAnchor.clone();
        this.aimAngles[0] = yaw;
        this.aimAngles[1] = 0f;

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
        if (phase == DeliveryPhase.DEPARTING) {
            return false;
        }
        if (isWaitingReceiver()) {
            return true;
        }
        if (isWaitingSender() && returnDelivery) {
            return hasCargo();
        }
        return isWaitingSender();
    }

    public boolean isPunchable() {
        return visual != null
                && phase != DeliveryPhase.DONE
                && phase != DeliveryPhase.IN_TRANSIT
                && phase != DeliveryPhase.DEPARTING;
    }

    public boolean canManageCargo(UUID playerId) {
        if (isWaitingReceiver()) {
            return playerId.equals(receiverId);
        }
        if (isWaitingSender() && returnDelivery) {
            return playerId.equals(senderId);
        }
        if (isWaitingSender()) {
            return playerId.equals(senderId);
        }
        return false;
    }

    public boolean isReturnDelivery() {
        return returnDelivery;
    }

    public boolean isLocalDelivery() {
        return localDelivery;
    }

    public UUID storedPackageId() {
        return storedPackageId;
    }

    public boolean isStoredDelivery() {
        return storedDelivery;
    }

    public void setStoredDelivery(boolean storedDelivery) {
        this.storedDelivery = storedDelivery;
    }

    public void setReturnDelivery(boolean returnDelivery) {
        this.returnDelivery = returnDelivery;
    }

    public void setLocalDelivery(boolean localDelivery) {
        this.localDelivery = localDelivery;
    }

    public void setStoredPackageId(UUID storedPackageId) {
        this.storedPackageId = storedPackageId;
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

    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    public void setOnStoreForReceiver(Runnable onStoreForReceiver) {
        this.onStoreForReceiver = onStoreForReceiver;
    }

    public void setOnStoreForSender(Runnable onStoreForSender) {
        this.onStoreForSender = onStoreForSender;
    }

    public void loadCargo(Map<Integer, ItemStack> items) {
        cargo.clear();
        if (items == null) {
            cargoLoaded = false;
            return;
        }
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            ItemStack item = entry.getValue();
            if (item != null && !item.isEmpty()) {
                cargo.put(entry.getKey(), item.clone());
            }
        }
        cargoLoaded = !cargo.isEmpty();
    }

    public Map<Integer, ItemStack> cargoBySlot() {
        Map<Integer, ItemStack> copy = new LinkedHashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : cargo.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    public List<ItemStack> cargo() {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : cargo.values()) {
            items.add(item.clone());
        }
        return items;
    }

    public boolean hasCargo() {
        return cargoLoaded && !cargo.isEmpty();
    }

    public void beginDeparture() {
        if (phase != DeliveryPhase.WAITING_SENDER || !cargoLoaded) {
            return;
        }
        departureTarget = DepartureTarget.RECEIVER;
        switchPhase(DeliveryPhase.DEPARTING);
    }

    public void beginLeaving(boolean withEffect) {
        leaveWithEffect = withEffect;
        if (phase == DeliveryPhase.DEPARTING || phase == DeliveryPhase.DONE) {
            return;
        }
        if (visual == null || phase == DeliveryPhase.IN_TRANSIT) {
            finish(withEffect);
            return;
        }
        departureTarget = DepartureTarget.LEAVE;
        switchPhase(DeliveryPhase.DEPARTING);
    }

    public void beginReturnToSender() {
        if (!hasCargo()) {
            beginLeaving(false);
            return;
        }
        if (visual == null) {
            returnCargoToSender();
            beginLeaving(false);
            return;
        }
        departureTarget = DepartureTarget.SENDER;
        switchPhase(DeliveryPhase.DEPARTING);
    }

    public List<ItemStack> takeCargoForMenu() {
        Map<Integer, ItemStack> items = cargoBySlot();
        cargo.clear();
        cargoLoaded = false;
        return new ArrayList<>(items.values());
    }

    public Map<Integer, ItemStack> takeCargoForMenuBySlot() {
        Map<Integer, ItemStack> items = cargoBySlot();
        cargo.clear();
        cargoLoaded = false;
        return items;
    }

    public void restoreCargo(Map<Integer, ItemStack> items) {
        loadCargo(items);
    }

    public void setReceiverMenuOpen(boolean open) {
        this.receiverMenuOpen = open;
    }

    public void setSenderMenuOpen(boolean open) {
        this.senderMenuOpen = open;
    }

    public void resetPartialPickupWait() {
        if (phase == DeliveryPhase.WAITING_RECEIVER
                || (returnDelivery && phase == DeliveryPhase.WAITING_SENDER)) {
            phaseTick = 0;
            phaseLimit = config.partialPickupWaitTicks();
        }
    }

    public void completeDelivery() {
        cargo.clear();
        cargoLoaded = false;
        beginLeaving(false);
    }

    public void returnRemainingCargo() {
        Player sender = Bukkit.getPlayer(senderId);
        if (sender != null && sender.isOnline() && hasCargo()) {
            beginReturnToSender();
            return;
        }
        returnCargoToSender();
        beginLeaving(false);
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
        abort();
    }

    public void abort() {
        if (phase == DeliveryPhase.DONE) {
            return;
        }
        if (visual != null) {
            visual.remove();
            visual = null;
        }
        finish(false);
    }

    private boolean continueTickingAfterLeave() {
        return phase != DeliveryPhase.DONE;
    }

    private boolean tickAssembly() {
        if (!tickSenderIdleTimeout()) {
            return continueTickingAfterLeave();
        }

        if (visual == null) {
            ensureVisual();
            return true;
        }

        phaseTick++;
        followSender();
        float progress = Math.min(1f, phaseTick / (float) Math.max(1, phaseLimit));
        float eased = easeOutCubic(progress);
        visual.updateAssembly(anchor, aimAngles[0], aimAngles[1], eased, phaseTick, assemblyStartMatrices);
        spawnDroneEffects();

        if (progress >= 1f) {
            ConfiguredSound.play(world, anchor, config.droneSoundAssemblyReady());
            world.spawnParticle(Particle.WITCH, anchor, 12, 0.4, 0.4, 0.4, 0.02);
            if (localDelivery) {
                if (returnDelivery) {
                    switchPhase(DeliveryPhase.WAITING_SENDER);
                    notifyReturnArrived();
                } else {
                    switchPhase(DeliveryPhase.WAITING_RECEIVER);
                    notifyReceiver();
                }
            } else {
                switchPhase(DeliveryPhase.WAITING_SENDER);
            }
        }
        return true;
    }

    private boolean tickWaitingSender() {
        if (returnDelivery) {
            return tickReturnPickupWait();
        }
        if (!tickSenderIdleTimeout()) {
            return continueTickingAfterLeave();
        }

        followSender();
        hoverAge++;
        applyHover();
        visual.update(anchor, aimAngles[0], aimAngles[1]);
        spawnDroneEffects();
        return true;
    }

    private boolean tickReturnPickupWait() {
        followSender();
        hoverAge++;
        applyHover();
        visual.update(anchor, aimAngles[0], aimAngles[1]);
        spawnDroneEffects();

        if (!receiverMenuOpen) {
            phaseTick++;
        }

        if (phaseTick >= phaseLimit) {
            returnCargoToSender();
            beginLeaving(false);
            return continueTickingAfterLeave();
        }
        return true;
    }

    private boolean tickSenderIdleTimeout() {
        if (senderMenuOpen) {
            return true;
        }
        senderIdleTicks++;
        if (senderIdleTicks < config.senderWaitTicks()) {
            return true;
        }
        if (onSenderTimeout != null) {
            onSenderTimeout.run();
            onSenderTimeout = null;
        }
        beginLeaving(false);
        return continueTickingAfterLeave();
    }

    private boolean tickDeparting() {
        phaseTick++;
        departureSpin += (float) Math.toRadians(config.departureSpinSpeed());
        anchor.add(0, config.departureRiseSpeed(), 0);
        hoverBase = anchor.clone();
        visual.update(anchor, aimAngles[0], aimAngles[1], departureSpin);
        spawnDroneEffects();

        if (phaseTick % 4 == 0) {
            world.spawnParticle(Particle.PORTAL, anchor, 3, 0.2, 0.2, 0.2, 0.01);
        }

        if (phaseTick >= phaseLimit) {
            return completeDepartureSequence();
        }
        return true;
    }

    private boolean completeDepartureSequence() {
        ConfiguredSound.play(world, anchor, config.droneSoundDeparturePrimary());
        ConfiguredSound.play(world, anchor, config.droneSoundDepartureSecondary());
        world.spawnParticle(Particle.POOF, anchor, 10, 0.35, 0.35, 0.35, 0.02);
        world.spawnParticle(Particle.REVERSE_PORTAL, anchor, 16, 0.4, 0.4, 0.4, 0.04);
        if (visual != null) {
            visual.remove();
            visual = null;
        }

        return switch (departureTarget) {
            case RECEIVER -> {
                switchPhase(DeliveryPhase.IN_TRANSIT);
                yield true;
            }
            case SENDER -> {
                returnDelivery = true;
                switchPhase(DeliveryPhase.IN_TRANSIT);
                yield true;
            }
            case LEAVE -> {
                finish(leaveWithEffect);
                yield false;
            }
        };
    }

    private boolean tickInTransit() {
        UUID targetId = returnDelivery ? senderId : receiverId;
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            if (returnDelivery) {
                returnCargoToSender();
            } else if (hasCargo() && onStoreForReceiver != null) {
                onStoreForReceiver.run();
            }
            cargo.clear();
            cargoLoaded = false;
            beginLeaving(false);
            return continueTickingAfterLeave();
        }

        world = target.getWorld();
        landingTarget = landingSpot(target);
        anchor = landingTarget.clone().add(0, config.arrivalHeight(), 0);
        hoverBase = anchor.clone();
        aimAngles[0] = target.getLocation().getYaw();
        aimAngles[1] = 0f;

        notifiedReceiver = false;
        switchPhase(DeliveryPhase.ARRIVING);
        return true;
    }

    private boolean tickArriving() {
        if (visual == null) {
            ensureVisual();
            return true;
        }

        phaseTick++;
        Player target = deliveryTargetPlayer();
        if (target == null || !target.isOnline()) {
            if (returnDelivery) {
                returnCargoToSender();
            } else if (hasCargo() && onStoreForReceiver != null) {
                onStoreForReceiver.run();
            }
            cargo.clear();
            cargoLoaded = false;
            beginLeaving(false);
            return continueTickingAfterLeave();
        }

        world = target.getWorld();
        landingTarget = landingSpot(target);

        float playerYaw = target.getLocation().getYaw();
        if (!followYawInitialized) {
            followYaw = playerYaw;
            followYawInitialized = true;
        } else if (Math.abs(wrapDegrees(playerYaw - followYaw)) >= config.followYawThreshold()) {
            followYaw = lerpYaw(followYaw, playerYaw, (float) config.followYawLerp());
        }

        Location hoverSpot = followSpotReceiver(target, followYaw);
        float progress = Math.min(1f, phaseTick / (float) Math.max(1, phaseLimit));
        float eased = easeOutCubic(progress);

        double startY = hoverSpot.getY() + config.arrivalHeight();
        double endY = hoverSpot.getY();
        double currentY = startY + (endY - startY) * eased;

        double followLerp = 0.1 + eased * 0.08;
        hoverBase.setX(hoverBase.getX() + (hoverSpot.getX() - hoverBase.getX()) * followLerp);
        hoverBase.setZ(hoverBase.getZ() + (hoverSpot.getZ() - hoverBase.getZ()) * followLerp);
        hoverBase.setY(currentY);
        anchor = hoverBase.clone();

        aimAngles[0] = lerpYaw(aimAngles[0], followYaw, 0.08f);
        aimAngles[1] = lerpFloat(aimAngles[1], 8f * (1f - eased), 0.1f);

        visual.update(anchor, aimAngles[0], aimAngles[1]);
        spawnDroneEffects();

        if (!notifiedReceiver) {
            if (returnDelivery) {
                notifyReturnArrived();
            } else {
                notifyReceiver();
            }
            notifiedReceiver = true;
        }

        if (progress >= 1f) {
            ConfiguredSound.play(world, anchor, config.droneSoundArrivalReady());
            world.spawnParticle(Particle.WITCH, anchor, 10, 0.3, 0.3, 0.3, 0.02);
            if (returnDelivery) {
                switchPhase(DeliveryPhase.WAITING_SENDER);
            } else {
                switchPhase(DeliveryPhase.WAITING_RECEIVER);
            }
        }
        return true;
    }

    private boolean tickWaitingReceiver() {
        followReceiver();
        hoverAge++;
        applyHover();
        visual.update(anchor, aimAngles[0], aimAngles[1]);
        spawnDroneEffects();

        if (!receiverMenuOpen) {
            phaseTick++;
        }

        if (phaseTick >= phaseLimit) {
            if (onReceiverTimeout != null) {
                onReceiverTimeout.run();
            }
            returnRemainingCargo();
            return phase != DeliveryPhase.DONE;
        }
        return true;
    }

    private void spawnDroneEffects() {
        spawnDroneParticles();
        spawnDroneSounds();
    }

    private void spawnDroneParticles() {
        if (visual == null || world == null || anchor == null || !config.droneParticlesEnabled()) {
            return;
        }
        particleTick++;
        if (particleTick % 2 != 0) {
            return;
        }
        visual.spawnAmbientParticles(world, anchor, aimAngles[0], aimAngles[1], particleTick);
    }

    private void spawnDroneSounds() {
        if (visual == null || world == null || anchor == null || !config.droneSoundsEnabled()) {
            return;
        }
        if (particleTick % config.droneSoundLoopIntervalTicks() != 0) {
            return;
        }
        ConfiguredSound.play(world, anchor, config.droneSoundLoop());
    }

    private void ensureVisual() {
        if (visual != null || world == null || anchor == null) {
            return;
        }
        visual = new DroneVisual(config, messages, world, anchor, receiverName, senderName);
        if (phase == DeliveryPhase.ASSEMBLING && assemblyStartMatrices == null) {
            assemblyStartMatrices = visual.createAssemblyStartMatrices();
        }
    }

    private void applyHover() {
        anchor.setX(hoverBase.getX());
        anchor.setY(hoverBase.getY());
        anchor.setZ(hoverBase.getZ());

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
        if (next == DeliveryPhase.ARRIVING || next == DeliveryPhase.WAITING_RECEIVER) {
            followYawInitialized = false;
        }
        if (anchor != null) {
            hoverBase = anchor.clone();
        }
        phaseLimit = switch (next) {
            case ASSEMBLING -> config.assemblyDurationTicks();
            case WAITING_SENDER -> returnDelivery
                    ? config.receiverWaitTicks()
                    : config.senderWaitTicks();
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

    private void returnCargoToSender() {
        if (cargo.isEmpty()) {
            return;
        }
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            if (onStoreForSender != null && !cargo.isEmpty()) {
                onStoreForSender.run();
            } else {
                Location dropLocation = landingTarget != null ? landingTarget : anchor;
                if (dropLocation != null && world != null) {
                    for (ItemStack item : cargo.values()) {
                        if (item != null && !item.isEmpty()) {
                            world.dropItemNaturally(dropLocation, item.clone());
                        }
                    }
                }
            }
            cargo.clear();
            cargoLoaded = false;
            return;
        }
        for (ItemStack item : cargo.values()) {
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
        sender.sendMessage(messages.component("package-returned-sender", receiverName));
        cargo.clear();
        cargoLoaded = false;
    }

    private void finish(boolean withEffect) {
        if (withEffect && anchor != null && world != null && visual != null) {
            world.spawnParticle(Particle.CLOUD, anchor, 10, 0.4, 0.4, 0.4, 0.02);
            world.spawnParticle(Particle.BLOCK, anchor, 12, 0.4, 0.4, 0.4, 0.04, Material.PURPLE_CONCRETE.createBlockData());
        }
        if (visual != null) {
            visual.remove();
            visual = null;
        }
        phase = DeliveryPhase.DONE;
        if (onFinished != null) {
            Runnable callback = onFinished;
            onFinished = null;
            callback.run();
        }
    }

    private void notifyReturnArrived() {
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            return;
        }
        sender.sendMessage(messages.component("package-return-arriving", receiverName));
        sender.showTitle(net.kyori.adventure.title.Title.title(
                messages.component("package-return-arriving-title"),
                messages.component("package-return-arriving-subtitle", receiverName),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(250),
                        java.time.Duration.ofMillis(3500),
                        java.time.Duration.ofMillis(750)
                )
        ));
    }

    private Player deliveryTargetPlayer() {
        return Bukkit.getPlayer(returnDelivery ? senderId : receiverId);
    }

    private Location landingSpot(Player player) {
        Location base = player.getLocation().clone();
        Vector direction = base.getDirection().setY(0);
        if (direction.lengthSquared() < 0.0001) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();
        return base.add(direction.multiply(config.landingDistance())).add(0, 0.2, 0);
    }

    private void followSender() {
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline() || !sender.getWorld().equals(world)) {
            return;
        }

        float playerYaw = sender.getLocation().getYaw();
        if (!followYawInitialized) {
            followYaw = playerYaw;
            followYawInitialized = true;
        } else if (Math.abs(wrapDegrees(playerYaw - followYaw)) >= config.followYawThreshold()) {
            followYaw = lerpYaw(followYaw, playerYaw, (float) config.followYawLerp());
        }

        Location target = followSpot(sender, followYaw);
        moveHoverBaseToward(target);
    }

    private void followReceiver() {
        Player receiver = Bukkit.getPlayer(receiverId);
        if (receiver == null || !receiver.isOnline() || !receiver.getWorld().equals(world)) {
            return;
        }

        float playerYaw = receiver.getLocation().getYaw();
        if (!followYawInitialized) {
            followYaw = playerYaw;
            followYawInitialized = true;
        } else if (Math.abs(wrapDegrees(playerYaw - followYaw)) >= config.followYawThreshold()) {
            followYaw = lerpYaw(followYaw, playerYaw, (float) config.followYawLerp());
        }

        Location target = followSpotReceiver(receiver, followYaw);
        moveHoverBaseToward(target);
    }

    private void moveHoverBaseToward(Location target) {
        double dx = target.getX() - hoverBase.getX();
        double dy = target.getY() - hoverBase.getY();
        double dz = target.getZ() - hoverBase.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        boolean chasing = horizontalDist > config.followIdleRadius() || Math.abs(dy) > 0.15;

        if (!chasing) {
            smoothAimWhileIdle();
            return;
        }

        double speed = config.followMaxSpeed();
        double horizontalLerp = Math.min(1.0, speed / Math.max(horizontalDist, 0.001));
        double verticalLerp = Math.abs(dy) <= 0.05
                ? 0.0
                : Math.min(1.0, (speed * 0.4) / Math.max(Math.abs(dy), 0.001));

        double moveX = dx * horizontalLerp;
        double moveY = dy * verticalLerp;
        double moveZ = dz * horizontalLerp;

        hoverBase.setX(hoverBase.getX() + moveX);
        hoverBase.setY(hoverBase.getY() + moveY);
        hoverBase.setZ(hoverBase.getZ() + moveZ);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        aimAngles[0] = lerpYaw(aimAngles[0], targetYaw, 0.07f);
        aimAngles[1] = lerpFloat(aimAngles[1], flightPitchToward(dx, dy, horizontalDist), 0.06f);
    }

    private float flightPitchToward(double dx, double dy, double horizontalDist) {
        float pitch = 0f;

        if (horizontalDist > config.followIdleRadius()) {
            float chaseAmount = (float) Math.min(1.0, (horizontalDist - config.followIdleRadius()) / 3.0);
            pitch += 5f * chaseAmount;
        }

        if (Math.abs(dy) > 0.08) {
            pitch += (float) -Math.toDegrees(Math.atan2(dy, Math.max(horizontalDist, 0.4))) * 0.35f;
        }

        return clampAngle(pitch, -8f, 8f);
    }

    private void smoothAimWhileIdle() {
        aimAngles[0] = lerpYaw(aimAngles[0], followYaw, 0.06f);
        aimAngles[1] = lerpFloat(aimAngles[1], 0f, 0.08f);
    }

    private Location followSpotReceiver(Player receiver, float yaw) {
        Location base = receiver.getLocation();
        Vector direction = yawToDirection(yaw);

        double side = config.followSideOffset();
        Vector lateral = side == 0.0
                ? new Vector(0, 0, 0)
                : new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(side);

        return base.clone()
                .add(direction.multiply(config.landingDistance()))
                .add(lateral)
                .add(0, hoverHeightAbovePlayer(receiver), 0);
    }

    private Location followSpot(Player sender, float yaw) {
        Location base = sender.getLocation();
        Vector direction = yawToDirection(yaw);

        double side = config.followSideOffset();
        Vector lateral = side == 0.0
                ? new Vector(0, 0, 0)
                : new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(side);

        return base.clone()
                .add(direction.multiply(config.spawnDistance()))
                .add(lateral)
                .add(0, hoverHeightAbovePlayer(sender), 0);
    }

    private static Vector yawToDirection(float yaw) {
        double radians = Math.toRadians(yaw);
        Vector direction = new Vector(-Math.sin(radians), 0, Math.cos(radians));
        if (direction.lengthSquared() < 0.0001) {
            return new Vector(0, 0, 1);
        }
        return direction.normalize();
    }

    private double hoverHeightAbovePlayer(Player player) {
        return player.getHeight() + config.spawnHeight();
    }

    private static float lerpYaw(float from, float to, float t) {
        float delta = wrapDegrees(to - from);
        return from + delta * t;
    }

    private static float lerpFloat(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static float clampAngle(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrapDegrees(float angle) {
        angle = (angle + 180f) % 360f;
        if (angle < 0f) {
            angle += 360f;
        }
        return angle - 180f;
    }

    private static float easeOutCubic(float progress) {
        return 1f - (1f - progress) * (1f - progress) * (1f - progress);
    }

    private enum DepartureTarget {
        RECEIVER,
        SENDER,
        LEAVE
    }

}
