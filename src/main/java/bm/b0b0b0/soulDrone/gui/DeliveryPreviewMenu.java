package bm.b0b0b0.soulDrone.gui;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.util.CargoLayout;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public final class DeliveryPreviewMenu implements InventoryHolder {

    private final Inventory inventory;
    private final List<Integer> cargoSlots;

    public DeliveryPreviewMenu(PluginConfig config, Component title, Map<Integer, ItemStack> cargo) {
        this.cargoSlots = config.sortedCargoSlots();
        this.inventory = Bukkit.createInventory(this, config.guiSize(), title);
        fillBackground(config);
        CargoLayout.populate(inventory, cargoSlots, cargo);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public boolean isCargoSlot(int slot) {
        return cargoSlots.contains(slot);
    }

    private void fillBackground(PluginConfig config) {
        Material fillerMaterial = config.fillerMaterial();
        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        filler.setItemMeta(meta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!cargoSlots.contains(slot)) {
                inventory.setItem(slot, filler);
            }
        }
    }

}
