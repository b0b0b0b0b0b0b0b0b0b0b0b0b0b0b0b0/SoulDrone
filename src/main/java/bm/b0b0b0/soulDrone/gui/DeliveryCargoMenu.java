package bm.b0b0b0.soulDrone.gui;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DeliveryCargoMenu implements InventoryHolder {

    private final UUID droneId;
    private final UUID senderId;
    private final String receiverName;
    private final Inventory inventory;
    private final Set<Integer> cargoSlots;

    public DeliveryCargoMenu(
            PluginConfig config,
            MessageService messages,
            UUID droneId,
            Player sender,
            String receiverName
    ) {
        this.droneId = droneId;
        this.senderId = sender.getUniqueId();
        this.receiverName = receiverName;
        this.cargoSlots = new HashSet<>(config.cargoSlots());

        Component title = messages.component("gui-title", receiverName);
        this.inventory = Bukkit.createInventory(this, config.guiSize(), title);
        fillBackground(config);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID droneId() {
        return droneId;
    }

    public UUID senderId() {
        return senderId;
    }

    public boolean isCargoSlot(int slot) {
        return cargoSlots.contains(slot);
    }

    public boolean hasCargo() {
        for (int slot : cargoSlots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack[] extractCargo() {
        ItemStack[] items = new ItemStack[cargoSlots.size()];
        int index = 0;
        for (int slot : cargoSlots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.isEmpty()) {
                items[index] = item.clone();
            }
            index++;
        }
        return items;
    }

    public void returnCargoToPlayer(Player player) {
        for (int slot : cargoSlots) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.isEmpty()) {
                continue;
            }
            var leftover = player.getInventory().addItem(item.clone());
            inventory.setItem(slot, null);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
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
