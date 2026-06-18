package bm.b0b0b0.soulDrone.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CargoLayout {

    private CargoLayout() {
    }

    public static List<Integer> sortedSlots(Collection<Integer> cargoSlots) {
        List<Integer> slots = new ArrayList<>(cargoSlots);
        Collections.sort(slots);
        return slots;
    }

    public static Map<Integer, ItemStack> extractBySlot(Inventory inventory, Collection<Integer> cargoSlots) {
        Map<Integer, ItemStack> cargo = new LinkedHashMap<>();
        for (int slot : sortedSlots(cargoSlots)) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.isEmpty()) {
                cargo.put(slot, item.clone());
            }
        }
        return cargo;
    }

    public static void populate(Inventory inventory, Collection<Integer> cargoSlots, Map<Integer, ItemStack> cargo) {
        if (cargo == null || cargo.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, ItemStack> entry : cargo.entrySet()) {
            if (!cargoSlots.contains(entry.getKey())) {
                continue;
            }
            ItemStack item = entry.getValue();
            if (item != null && !item.isEmpty()) {
                inventory.setItem(entry.getKey(), item.clone());
            }
        }
    }

    public static boolean hasItems(Map<Integer, ItemStack> cargo) {
        if (cargo == null || cargo.isEmpty()) {
            return false;
        }
        for (ItemStack item : cargo.values()) {
            if (item != null && !item.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static Map<Integer, ItemStack> clone(Map<Integer, ItemStack> cargo) {
        Map<Integer, ItemStack> copy = new LinkedHashMap<>();
        if (cargo == null) {
            return copy;
        }
        for (Map.Entry<Integer, ItemStack> entry : cargo.entrySet()) {
            ItemStack item = entry.getValue();
            if (item != null && !item.isEmpty()) {
                copy.put(entry.getKey(), item.clone());
            }
        }
        return copy;
    }

    public static Map<Integer, ItemStack> fromLegacyList(Collection<Integer> cargoSlots, List<ItemStack> items) {
        Map<Integer, ItemStack> cargo = new LinkedHashMap<>();
        if (items == null || items.isEmpty()) {
            return cargo;
        }
        List<Integer> slots = sortedSlots(cargoSlots);
        int slotIndex = 0;
        for (ItemStack item : items) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            if (slotIndex >= slots.size()) {
                break;
            }
            cargo.put(slots.get(slotIndex), item.clone());
            slotIndex++;
        }
        return cargo;
    }

    public static List<ItemStack> toOrderedList(Collection<Integer> cargoSlots, Map<Integer, ItemStack> cargo) {
        List<ItemStack> items = new ArrayList<>();
        if (cargo == null || cargo.isEmpty()) {
            return items;
        }
        for (int slot : sortedSlots(cargoSlots)) {
            ItemStack item = cargo.get(slot);
            if (item != null && !item.isEmpty()) {
                items.add(item.clone());
            }
        }
        return items;
    }

}
