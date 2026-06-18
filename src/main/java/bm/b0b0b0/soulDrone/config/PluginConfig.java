package bm.b0b0b0.soulDrone.config;

import bm.b0b0b0.soulDrone.config.settings.GuiDeliverySettings;
import bm.b0b0b0.soulDrone.config.settings.SoulDroneSettings;
import bm.b0b0b0.soulDrone.drone.DroneVisual;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PluginConfig {

    private final SoulDroneSettings settings;
    private final GuiDeliverySettings guiSettings;
    private final List<DroneVisual.SegmentSpec> segmentSpecs;
    private final Set<Integer> cargoSlots;
    public PluginConfig(ConfigurationLoader loader) {
        this.settings = loader.settings();
        this.guiSettings = loader.guiSettings();
        this.segmentSpecs = parseSegments(settings.segments);
        this.cargoSlots = new HashSet<>(guiSettings.cargoSlots);
    }

    public double sendCost() {
        return Math.max(0.0, settings.sendCost);
    }

    public boolean requireVault() {
        return settings.requireVault;
    }

    public String sqliteFile() {
        return settings.sqliteFile;
    }

    public int databasePoolSize() {
        return Math.max(1, settings.databasePoolSize);
    }

    public long packageStorageMillis() {
        return Math.max(3600000L, (long) (settings.packageStorageDays * 86400000.0));
    }

    public boolean allowOfflineSend() {
        return settings.allowOfflineSend;
    }

    public boolean autoAcceptOffline() {
        return settings.autoAcceptOffline;
    }

    public String claimSubcommand() {
        return settings.claimSubcommand;
    }

    public String refuseSubcommand() {
        return settings.refuseSubcommand;
    }

    public boolean requireReceiverAccept() {
        return settings.requireReceiverAccept;
    }

    public int requestWaitTicks() {
        return Math.max(20, (int) Math.round(settings.requestWaitSeconds * 20.0));
    }

    public int senderWaitTicks() {
        return Math.max(20, (int) Math.round(settings.senderWaitSeconds * 20.0));
    }

    public int receiverWaitTicks() {
        return Math.max(20, (int) Math.round(settings.receiverWaitSeconds * 20.0));
    }

    public int partialPickupWaitTicks() {
        return Math.max(20, (int) Math.round(settings.partialPickupWaitSeconds * 20.0));
    }

    public double partialPickupWaitSeconds() {
        return Math.max(1.0, settings.partialPickupWaitSeconds);
    }

    public double spawnDistance() {
        return Math.max(1.0, settings.spawnDistance);
    }

    public double spawnHeight() {
        return Math.max(0.2, settings.spawnHeight);
    }

    public double followSideOffset() {
        return settings.followSideOffset;
    }

    public double followIdleRadius() {
        return Math.max(0.5, settings.followIdleRadius);
    }

    public double followMaxSpeed() {
        return Math.max(0.02, Math.min(0.5, settings.followMaxSpeed));
    }

    public double followYawThreshold() {
        return Math.max(5.0, settings.followYawThreshold);
    }

    public double followYawLerp() {
        return Math.max(0.02, Math.min(1.0, settings.followYawLerp));
    }

    public double followLerp() {
        return Math.max(0.05, Math.min(1.0, settings.followLerp));
    }

    public double landingDistance() {
        return Math.max(1.0, settings.landingDistance);
    }

    public double arrivalHeight() {
        return Math.max(2.0, settings.arrivalHeight);
    }

    public double departureRiseSpeed() {
        return settings.departureRiseSpeed;
    }

    public double arrivalDescentSpeed() {
        return settings.arrivalDescentSpeed;
    }

    public int assemblyDurationTicks() {
        return Math.max(1, settings.assemblyDurationTicks);
    }

    public int departureDurationTicks() {
        return Math.max(1, settings.departureDurationTicks);
    }

    public int arrivalDurationTicks() {
        return Math.max(1, settings.arrivalDurationTicks);
    }

    public double bobAmplitude() {
        return settings.bobAmplitude;
    }

    public double bobSpeed() {
        return settings.bobSpeed;
    }

    public double swayAmplitude() {
        return settings.swayAmplitude;
    }

    public double swaySpeed() {
        return settings.swaySpeed;
    }

    public double departureSpinSpeed() {
        return settings.departureSpinSpeed;
    }

    public float blockScale() {
        return Math.max(0.2f, Math.min(1.5f, settings.blockScale));
    }

    public boolean cargoPreviewOnSneak() {
        return settings.cargoPreviewOnSneak;
    }

    public boolean droneParticlesEnabled() {
        return settings.droneParticlesEnabled;
    }

    public boolean droneSoundsEnabled() {
        return settings.droneSoundsEnabled;
    }

    public int droneSoundLoopIntervalTicks() {
        return Math.max(1, settings.droneSoundLoopIntervalTicks);
    }

    public SoulDroneSettings.DroneSoundEntry droneSoundLoop() {
        return soundEntry(settings.droneSoundLoop, "ENTITY_BEE_LOOP", 0.65f, 1.12f);
    }

    public SoulDroneSettings.DroneSoundEntry droneSoundAssemblyReady() {
        return soundEntry(settings.droneSoundAssemblyReady, "BLOCK_AMETHYST_BLOCK_CHIME", 0.8f, 1.2f);
    }

    public SoulDroneSettings.DroneSoundEntry droneSoundArrivalReady() {
        return soundEntry(settings.droneSoundArrivalReady, "BLOCK_AMETHYST_BLOCK_CHIME", 0.9f, 0.9f);
    }

    public SoulDroneSettings.DroneSoundEntry droneSoundDeparturePrimary() {
        return soundEntry(settings.droneSoundDeparturePrimary, "BLOCK_BEACON_DEACTIVATE", 0.55f, 1.35f);
    }

    public SoulDroneSettings.DroneSoundEntry droneSoundDepartureSecondary() {
        return soundEntry(settings.droneSoundDepartureSecondary, "ENTITY_ENDERMAN_TELEPORT", 0.45f, 0.85f);
    }

    public boolean dronePunchEnabled() {
        return settings.dronePunchEnabled;
    }

    public double dronePunchDamage() {
        return Math.max(0.0, settings.dronePunchDamage);
    }

    public double dronePunchKnockback() {
        return Math.max(0.0, settings.dronePunchKnockback);
    }

    public int dronePunchCooldownTicks() {
        return Math.max(0, settings.dronePunchCooldownTicks);
    }

    public double dronePunchBroadcastRadius() {
        return Math.max(1.0, settings.dronePunchBroadcastRadius);
    }

    public double dronePunchReach() {
        return Math.max(1.5, settings.dronePunchReach);
    }

    public String accentHex() {
        String hex = settings.accentHex;
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        return hex;
    }

    public float labelOffsetY() {
        return settings.labelOffsetY;
    }

    public int labelBackgroundArgb() {
        return settings.labelBackgroundArgb;
    }

    public boolean labelEnabled() {
        return settings.labelEnabled;
    }

    public float hitboxWidth() {
        return Math.max(0.5f, settings.hitboxWidth);
    }

    public float hitboxHeight() {
        return Math.max(0.5f, settings.hitboxHeight);
    }

    public double clickRadius() {
        return Math.max(1.0, settings.clickRadius);
    }

    public List<DroneVisual.SegmentSpec> segmentSpecs() {
        return segmentSpecs;
    }

    public int guiSize() {
        int size = guiSettings.size;
        if (size < 9) {
            return 9;
        }
        if (size > 54) {
            return 54;
        }
        return size - (size % 9);
    }

    public Material fillerMaterial() {
        Material material = Material.matchMaterial(guiSettings.fillerMaterial);
        if (material == null || !material.isItem()) {
            return Material.GRAY_STAINED_GLASS_PANE;
        }
        return material;
    }

    public List<Integer> sortedCargoSlots() {
        List<Integer> slots = new ArrayList<>(cargoSlots);
        slots.sort(Integer::compareTo);
        return slots;
    }

    public Set<Integer> cargoSlots() {
        return cargoSlots;
    }

    public String sendPermission() {
        return settings.sendPermission;
    }

    public String acceptPermission() {
        return settings.acceptPermission;
    }

    public String denyPermission() {
        return settings.denyPermission;
    }

    public String acceptSubcommand() {
        return settings.acceptSubcommand;
    }

    public String denySubcommand() {
        return settings.denySubcommand;
    }

    public String toggleSubcommand() {
        return settings.toggleSubcommand;
    }

    public boolean defaultReceivesDrones() {
        return settings.defaultReceivesDrones;
    }

    public String togglePermission() {
        return settings.togglePermission;
    }

    public String openPermission() {
        return settings.openPermission;
    }

    public String receivePermission() {
        return settings.receivePermission;
    }

    public String bypassCostPermission() {
        return settings.bypassCostPermission;
    }

    public String language() {
        return settings.language;
    }

    private static List<DroneVisual.SegmentSpec> parseSegments(List<SoulDroneSettings.DroneSegmentEntry> entries) {
        List<DroneVisual.SegmentSpec> specs = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            for (SoulDroneSettings.DroneSegmentEntry entry : SoulDroneSettings.defaultSegments()) {
                specs.add(toSpec(entry));
            }
            return specs;
        }
        for (SoulDroneSettings.DroneSegmentEntry entry : entries) {
            specs.add(toSpec(entry));
        }
        return specs;
    }

    private static DroneVisual.SegmentSpec toSpec(SoulDroneSettings.DroneSegmentEntry entry) {
        Material material = Material.matchMaterial(entry.material);
        if (material == null || !material.isBlock()) {
            material = Material.PURPLE_CONCRETE;
        }
        return new DroneVisual.SegmentSpec(entry.forward, entry.lateral, entry.vertical, material);
    }

    private static SoulDroneSettings.DroneSoundEntry soundEntry(
            SoulDroneSettings.DroneSoundEntry entry,
            String defaultSound,
            float defaultVolume,
            float defaultPitch
    ) {
        if (entry == null || entry.sound == null || entry.sound.isBlank()) {
            return new SoulDroneSettings.DroneSoundEntry(defaultSound, defaultVolume, defaultPitch);
        }
        return entry;
    }

}
