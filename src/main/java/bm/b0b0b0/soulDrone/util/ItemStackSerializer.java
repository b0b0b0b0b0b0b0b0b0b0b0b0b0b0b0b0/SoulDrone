package bm.b0b0b0.soulDrone.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemStackSerializer {

    private static final int SLOTTED_MARKER = -1;

    private ItemStackSerializer() {
    }

    public static String serializeCargo(Map<Integer, ItemStack> cargo) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream stream = new BukkitObjectOutputStream(output)) {
            stream.writeInt(SLOTTED_MARKER);
            stream.writeInt(cargo.size());
            for (Map.Entry<Integer, ItemStack> entry : cargo.entrySet()) {
                stream.writeInt(entry.getKey());
                stream.writeObject(entry.getValue());
            }
        }
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    @SuppressWarnings("unchecked")
    public static Map<Integer, ItemStack> deserializeCargo(String encoded, Collection<Integer> cargoSlots)
            throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(input)) {
            int marker = stream.readInt();
            if (marker == SLOTTED_MARKER) {
                int size = stream.readInt();
                Map<Integer, ItemStack> cargo = new LinkedHashMap<>();
                for (int index = 0; index < size; index++) {
                    int slot = stream.readInt();
                    ItemStack item = (ItemStack) stream.readObject();
                    if (item != null && !item.isEmpty()) {
                        cargo.put(slot, item);
                    }
                }
                return cargo;
            }

            int size = marker;
            List<ItemStack> items = new ArrayList<>(Math.max(0, size));
            for (int index = 0; index < size; index++) {
                items.add((ItemStack) stream.readObject());
            }
            return CargoLayout.fromLegacyList(cargoSlots, items);
        }
    }

}
