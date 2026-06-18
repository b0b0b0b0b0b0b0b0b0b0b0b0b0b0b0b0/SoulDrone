package bm.b0b0b0.soulDrone.database;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class DatabaseBootstrap {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private HikariDataSource dataSource;
    private ExecutorService executor;

    public DatabaseBootstrap(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public CompletableFuture<Void> start() {
        return runAsync(() -> {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setPoolName("SoulDrone");
            hikariConfig.setMaximumPoolSize(config.databasePoolSize());

            File file = new File(plugin.getDataFolder(), config.sqliteFile());
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");

            dataSource = new HikariDataSource(hikariConfig);
            new MigrationRunner(dataSource).migrate();
        });
    }

    public Connection connection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("Database is not ready");
        }
        return dataSource.getConnection();
    }

    public <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor());
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor());
    }

    public void runSync(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private ExecutorService executor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(Math.max(1, config.databasePoolSize()));
        }
        return executor;
    }

}
