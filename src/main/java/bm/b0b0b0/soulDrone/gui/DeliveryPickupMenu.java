package bm.b0b0b0.soulDrone.gui;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
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

public final class DeliveryPickupMenu implements InventoryHolder {

    private final UUID droneId;
    private final UUID receiverId;
    private final UUID senderId;
    private final String senderName;
    private final Inventory inventory;
    private final List<Integer> cargoSlots;

    public DeliveryPickupMenu(
            PluginConfig config,
            MessageService messages,
            UUID droneId,
            UUID receiverId,
            UUID senderId,
            String senderName,
            Map<Integer, ItemStack> cargo
    ) {
        this.droneId = droneId;
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.cargoSlots = config.sortedCargoSlots();

        Component title = messages.component("gui-pickup-title", senderName);
        this.inventory = Bukkit.createInventory(this, config.guiSize(), title);
        fillBackground(config);
        CargoLayout.populate(inventory, cargoSlots, cargo);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID droneId() {
        return droneId;
    }

    public UUID receiverId() {
        return receiverId;
    }

    public UUID senderId() {
        return senderId;
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
