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

    public double spawnDistance() {
        return Math.max(1.0, settings.spawnDistance);
    }

    public double spawnHeight() {
        return Math.max(0.5, settings.spawnHeight);
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

}
