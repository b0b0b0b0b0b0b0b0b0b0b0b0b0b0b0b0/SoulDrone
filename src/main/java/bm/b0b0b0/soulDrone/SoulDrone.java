package bm.b0b0b0.soulDrone;

import bm.b0b0b0.soulDrone.command.SendCommand;
import bm.b0b0b0.soulDrone.config.ConfigurationLoader;
import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.data.ReceiverToggleStore;
import bm.b0b0b0.soulDrone.drone.DroneManager;
import bm.b0b0b0.soulDrone.economy.VaultEconomyService;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.listener.DeliveryListener;
import bm.b0b0b0.soulDrone.service.DeliveryService;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulDrone extends JavaPlugin {

    private DroneManager droneManager;
    private ReceiverToggleStore receiverToggleStore;

    @Override
    public void onEnable() {
        ConfigurationLoader configurationLoader = new ConfigurationLoader(this);
        PluginConfig pluginConfig = new PluginConfig(configurationLoader);
        MessageService messageService = new MessageService(this, pluginConfig.language());
        VaultEconomyService vaultEconomyService = new VaultEconomyService(this);

        droneManager = new DroneManager();
        droneManager.start(this);

        ReceiverToggleStore receiverToggleStore = new ReceiverToggleStore(
                this,
                pluginConfig.defaultReceivesDrones()
        );
        this.receiverToggleStore = receiverToggleStore;

        DeliveryService deliveryService = new DeliveryService(
                this,
                pluginConfig,
                messageService,
                vaultEconomyService,
                droneManager,
                receiverToggleStore
        );

        SendCommand sendCommand = new SendCommand(pluginConfig, messageService, deliveryService);
        getCommand("send").setExecutor(sendCommand);
        getCommand("send").setTabCompleter(sendCommand);

        getServer().getPluginManager().registerEvents(
                new DeliveryListener(pluginConfig, deliveryService, droneManager),
                this
        );
    }

    @Override
    public void onDisable() {
        if (droneManager != null) {
            droneManager.shutdown();
        }
        if (receiverToggleStore != null) {
            receiverToggleStore.save();
        }
    }

}
