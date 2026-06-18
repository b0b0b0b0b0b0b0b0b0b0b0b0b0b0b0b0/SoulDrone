package bm.b0b0b0.soulDrone;

import bm.b0b0b0.soulDrone.command.SendCommand;
import bm.b0b0b0.soulDrone.config.ConfigurationLoader;
import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.data.ReceiverToggleStore;
import bm.b0b0b0.soulDrone.database.DatabaseBootstrap;
import bm.b0b0b0.soulDrone.drone.DroneManager;
import bm.b0b0b0.soulDrone.economy.VaultEconomyService;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.listener.DeliveryListener;
import bm.b0b0b0.soulDrone.listener.PlayerJoinListener;
import bm.b0b0b0.soulDrone.listener.PlayerQuitListener;
import bm.b0b0b0.soulDrone.repository.SqlPackageRepository;
import bm.b0b0b0.soulDrone.service.DeliveryService;
import bm.b0b0b0.soulDrone.service.StoredPackageService;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulDrone extends JavaPlugin {

    private DroneManager droneManager;
    private ReceiverToggleStore receiverToggleStore;
    private DatabaseBootstrap databaseBootstrap;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder");
        }

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
        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(deliveryService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new PlayerQuitListener(deliveryService),
                this
        );

        databaseBootstrap = new DatabaseBootstrap(this, pluginConfig);
        databaseBootstrap.start().whenComplete((ignored, error) -> getServer().getScheduler().runTask(this, () -> {
            if (error != null) {
                getLogger().severe("Package database failed to start: " + error.getMessage());
                error.printStackTrace();
                return;
            }
            SqlPackageRepository repository = new SqlPackageRepository(databaseBootstrap, pluginConfig);
            StoredPackageService storedPackageService = new StoredPackageService(
                    this,
                    pluginConfig,
                    messageService,
                    repository
            );
            storedPackageService.setDeliveryService(deliveryService);
            deliveryService.setStoredPackageService(storedPackageService);
            getLogger().info("Package storage ready");
        }));

        getServer().getScheduler().runTask(this, () ->
                vaultEconomyService.logDeliveryPricing(pluginConfig.sendCost()));
    }

    @Override
    public void onDisable() {
        if (droneManager != null) {
            droneManager.shutdown();
        }
        if (receiverToggleStore != null) {
            receiverToggleStore.save();
        }
        if (databaseBootstrap != null) {
            databaseBootstrap.shutdown();
        }
    }

}
