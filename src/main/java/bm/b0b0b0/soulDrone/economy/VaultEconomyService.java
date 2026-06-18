package bm.b0b0b0.soulDrone.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultEconomyService {

    private final Economy economy;

    public VaultEconomyService(JavaPlugin plugin) {
        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = registration != null ? registration.getProvider() : null;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        if (economy == null || amount <= 0.0) {
            return true;
        }
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null || amount <= 0.0) {
            return true;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null || amount <= 0.0) {
            return true;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (economy == null) {
            return String.valueOf(amount);
        }
        return economy.format(amount);
    }

}
