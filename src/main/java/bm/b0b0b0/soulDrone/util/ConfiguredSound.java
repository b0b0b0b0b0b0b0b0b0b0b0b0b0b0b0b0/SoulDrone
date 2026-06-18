package bm.b0b0b0.soulDrone.util;

import bm.b0b0b0.soulDrone.config.settings.SoulDroneSettings.DroneSoundEntry;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

import java.util.Locale;

public final class ConfiguredSound {

    private ConfiguredSound() {
    }

    public static void play(World world, Location location, DroneSoundEntry entry) {
        if (world == null || location == null || entry == null) {
            return;
        }
        String raw = entry.sound;
        if (raw == null || raw.isBlank()) {
            return;
        }

        float volume = Math.max(0f, entry.volume);
        float pitch = Math.max(0.01f, Math.min(2f, entry.pitch));
        String key = raw.trim();

        Sound enumSound = resolveEnum(key);
        if (enumSound != null) {
            world.playSound(location, enumSound, volume, pitch);
            return;
        }
        world.playSound(location, normalizeNamespaced(key), volume, pitch);
    }

    private static Sound resolveEnum(String key) {
        if (key.contains(":")) {
            return null;
        }
        try {
            return Sound.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalizeNamespaced(String key) {
        if (key.contains(":")) {
            return key.toLowerCase(Locale.ROOT);
        }
        return "minecraft:" + key.toLowerCase(Locale.ROOT).replace('_', '.');
    }

}
