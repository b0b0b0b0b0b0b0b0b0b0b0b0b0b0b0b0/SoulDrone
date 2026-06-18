package bm.b0b0b0.soulDrone.config;

import bm.b0b0b0.soulDrone.config.settings.GuiDeliverySettings;
import bm.b0b0b0.soulDrone.config.settings.SoulDroneSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class ConfigurationLoader {

    private final Path configPath;
    private final Path guiPath;
    private final SoulDroneSettings settings;
    private final GuiDeliverySettings guiSettings;

    public ConfigurationLoader(JavaPlugin plugin) {
        Path dataFolder = plugin.getDataFolder().toPath();
        plugin.getDataFolder().mkdirs();

        this.configPath = dataFolder.resolve("config.yml");
        settings = new SoulDroneSettings();
        if (configPath.toFile().exists()) {
            settings.reload(configPath);
        }
        settings.save(configPath);

        Path guiDir = dataFolder.resolve("gui");
        guiDir.toFile().mkdirs();
        this.guiPath = guiDir.resolve("delivery.yml");
        guiSettings = new GuiDeliverySettings();
        if (guiPath.toFile().exists()) {
            guiSettings.reload(guiPath);
        }
        guiSettings.save(guiPath);
    }

    public void reload() {
        if (configPath.toFile().exists()) {
            settings.reload(configPath);
        }
        settings.save(configPath);

        if (guiPath.toFile().exists()) {
            guiSettings.reload(guiPath);
        }
        guiSettings.save(guiPath);
    }

    public SoulDroneSettings settings() {
        return settings;
    }

    public GuiDeliverySettings guiSettings() {
        return guiSettings;
    }

}
