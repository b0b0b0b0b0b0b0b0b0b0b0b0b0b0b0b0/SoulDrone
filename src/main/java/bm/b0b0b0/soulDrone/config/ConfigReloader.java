package bm.b0b0b0.soulDrone.config;

import bm.b0b0b0.soulDrone.economy.VaultEconomyService;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.service.DeliveryService;
import bm.b0b0b0.soulDrone.zone.BlockedZoneService;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigReloader {

    private final JavaPlugin plugin;
    private final ConfigurationLoader configurationLoader;
    private final PluginConfig config;
    private final MessageService messages;
    private final BlockedZoneService blockedZones;
    private final VaultEconomyService economy;
    private final DeliveryService deliveryService;

    public ConfigReloader(
            JavaPlugin plugin,
            ConfigurationLoader configurationLoader,
            PluginConfig config,
            MessageService messages,
            BlockedZoneService blockedZones,
            VaultEconomyService economy,
            DeliveryService deliveryService
    ) {
        this.plugin = plugin;
        this.configurationLoader = configurationLoader;
        this.config = config;
        this.messages = messages;
        this.blockedZones = blockedZones;
        this.economy = economy;
        this.deliveryService = deliveryService;
    }

    public boolean reload(CommandSender sender) {
        try {
            configurationLoader.reload();
            config.reloadDerived();
            messages.reload(plugin, config.language());
            blockedZones.reload();
            deliveryService.reconcileAfterReload();
            economy.refreshEconomy();
            economy.logDeliveryPricing(config.sendCost());
            sender.sendMessage(messages.component("reload-success"));
            plugin.getLogger().info("Configuration reloaded by " + sender.getName());
            return true;
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to reload configuration: " + exception.getMessage());
            exception.printStackTrace();
            sender.sendMessage(messages.component("reload-failed", exception.getMessage()));
            return false;
        }
    }

}
