package bm.b0b0b0.soulDrone.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultEconomyService {

    private final JavaPlugin plugin;
    private Economy economy;
    private Boolean economyReady;

    public VaultEconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void refreshEconomy() {
        economyReady = null;
        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = registration != null ? registration.getProvider() : null;
    }

    public boolean isAvailable() {
        return checkEconomyReady();
    }

    public boolean chargesDelivery(double sendCost) {
        return sendCost > 0.0 && checkEconomyReady();
    }

    public void logDeliveryPricing(double sendCost) {
        refreshEconomy();
        if (sendCost <= 0.0) {
            plugin.getLogger().info("[SoulDrone] sendCost=0 — доставка бесплатная.");
            return;
        }
        if (checkEconomyReady()) {
            plugin.getLogger().info("[SoulDrone] Стоимость доставки: " + format(sendCost) + " (Vault).");
            return;
        }
        plugin.getLogger().warning(
                "[SoulDrone] sendCost=" + sendCost + ", но Vault Economy не найден — доставка бесплатная. "
                        + "Поставь Vault + плагин экономики (Essentials, CMI и т.д.) и перезапусти сервер."
        );
    }

    public boolean has(Player player, double amount) {
        if (!chargesDelivery(amount)) {
            return true;
        }
        try {
            return economy.has(player, amount);
        } catch (Exception ex) {
            plugin.getLogger().warning("[SoulDrone] Economy.has failed, delivery is free: " + ex.getMessage());
            economyReady = false;
            return true;
        }
    }

    public boolean withdraw(Player player, double amount) {
        if (!chargesDelivery(amount)) {
            return true;
        }
        try {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Exception ex) {
            plugin.getLogger().warning("[SoulDrone] Economy.withdraw failed: " + ex.getMessage());
            economyReady = false;
            return false;
        }
    }

    public boolean deposit(Player player, double amount) {
        if (!checkEconomyReady() || amount <= 0.0) {
            return true;
        }
        try {
            return economy.depositPlayer(player, amount).transactionSuccess();
        } catch (Exception ex) {
            plugin.getLogger().warning("[SoulDrone] Economy.deposit failed: " + ex.getMessage());
            economyReady = false;
            return false;
        }
    }

    public String format(double amount) {
        if (!checkEconomyReady()) {
            return String.valueOf(amount);
        }
        try {
            return economy.format(amount);
        } catch (Exception ex) {
            economyReady = false;
            return String.valueOf(amount);
        }
    }

    private boolean checkEconomyReady() {
        if (economyReady != null) {
            return economyReady;
        }
        if (economy == null) {
            RegisteredServiceProvider<Economy> registration =
                    plugin.getServer().getServicesManager().getRegistration(Economy.class);
            economy = registration != null ? registration.getProvider() : null;
        }
        if (economy == null) {
            economyReady = false;
            return false;
        }
        try {
            economy.format(1.0);
            economyReady = true;
            return true;
        } catch (Exception ignored) {
            economyReady = false;
            return false;
        }
    }

}
