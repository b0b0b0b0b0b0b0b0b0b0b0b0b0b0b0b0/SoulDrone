package bm.b0b0b0.soulDrone.gui;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
import bm.b0b0b0.soulDrone.model.StoredPackageKind;
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
import java.util.UUID;

public final class StoredPickupMenu implements InventoryHolder {

    private final UUID packageId;
    private final UUID ownerId;
    private final StoredPackageKind kind;
    private final String counterpartName;
    private final Inventory inventory;
    private final List<Integer> cargoSlots;

    public StoredPickupMenu(
            PluginConfig config,
            MessageService messages,
            UUID packageId,
            UUID ownerId,
            StoredPackageKind kind,
            String counterpartName,
            Map<Integer, ItemStack> cargo
    ) {
        this.packageId = packageId;
        this.ownerId = ownerId;
        this.kind = kind;
        this.counterpartName = counterpartName;
        this.cargoSlots = config.sortedCargoSlots();

        Component title = kind == StoredPackageKind.TO_RECEIVER
                ? messages.component("gui-pickup-title", counterpartName)
                : messages.component("gui-return-title", counterpartName);
        this.inventory = Bukkit.createInventory(this, config.guiSize(), title);
        fillBackground(config);
        CargoLayout.populate(inventory, cargoSlots, cargo);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID packageId() {
        return packageId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public StoredPackageKind kind() {
        return kind;
    }

    public boolean isCargoSlot(int slot) {
        return cargoSlots.contains(slot);
    }

    public boolean hasRemainingCargo() {
        return CargoLayout.hasItems(collectRemainingCargoBySlot());
    }

    public Map<Integer, ItemStack> collectRemainingCargoBySlot() {
        Map<Integer, ItemStack> remaining = CargoLayout.extractBySlot(inventory, cargoSlots);
        for (int slot : cargoSlots) {
            inventory.setItem(slot, null);
        }
        return remaining;
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
