package bm.b0b0b0.soulDrone.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class MessageService {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Map<String, String> messages = new HashMap<>();
    private final List<String> dronePunchLines = new ArrayList<>();

    public MessageService(JavaPlugin plugin, String language) {
        load(plugin, language);
    }

    public Component component(String key, Object... args) {
        String raw = messages.getOrDefault(key, key);
        if (args.length > 0) {
            raw = String.format(raw, args);
        }
        return MINI_MESSAGE.deserialize(raw);
    }

    public String raw(String key, Object... args) {
        String raw = messages.getOrDefault(key, key);
        if (args.length > 0) {
            raw = String.format(raw, args);
        }
        return raw;
    }

    public Component randomDronePunchLine() {
        if (dronePunchLines.isEmpty()) {
            return component("drone-punch-fallback");
        }
        String line = dronePunchLines.get(ThreadLocalRandom.current().nextInt(dronePunchLines.size()));
        return MINI_MESSAGE.deserialize(line);
    }

    public void reload(JavaPlugin plugin, String language) {
        messages.clear();
        dronePunchLines.clear();
        load(plugin, language);
    }

    private void load(JavaPlugin plugin, String language) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.getLogger().warning("Could not create lang directory");
        }

        String fileName = language + ".yml";
        File langFile = new File(langDir, fileName);
        if (!langFile.exists()) {
            copyDefault(plugin, "lang/" + fileName, langFile);
        }
        if (!langFile.exists()) {
            copyDefault(plugin, "lang/en.yml", langFile);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(langFile);
        for (String key : yaml.getKeys(true)) {
            if (yaml.isString(key)) {
                messages.put(key, yaml.getString(key));
            }
        }
        dronePunchLines.clear();
        for (String line : yaml.getStringList("drone-punch-lines")) {
            if (line != null && !line.isBlank()) {
                dronePunchLines.add(line);
            }
        }
    }

    private void copyDefault(JavaPlugin plugin, String resourcePath, File target) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return;
            }
            Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to copy lang file: " + resourcePath, exception);
        }
    }

}
