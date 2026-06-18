package bm.b0b0b0.soulDrone.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ReceiverToggleStore {

    private final JavaPlugin plugin;
    private final boolean defaultReceives;
    private final File file;
    private final Map<UUID, Boolean> toggles = new HashMap<>();

    public ReceiverToggleStore(JavaPlugin plugin, boolean defaultReceives) {
        this.plugin = plugin;
        this.defaultReceives = defaultReceives;
        this.file = new File(plugin.getDataFolder(), "receiver-toggles.yml");
        load();
    }

    public boolean acceptsDeliveries(UUID playerId) {
        return toggles.getOrDefault(playerId, defaultReceives);
    }

    public boolean toggle(UUID playerId) {
        boolean next = !acceptsDeliveries(playerId);
        toggles.put(playerId, next);
        save();
        return next;
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Boolean> entry : toggles.entrySet()) {
            yaml.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save receiver toggles", exception);
        }
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                toggles.put(playerId, yaml.getBoolean(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

}
