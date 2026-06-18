package bm.b0b0b0.soulDrone.model;

import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public final class StoredPackage {

    private final UUID id;
    private final StoredPackageKind kind;
    private final UUID senderId;
    private final String senderName;
    private final UUID receiverId;
    private final String receiverName;
    private final Map<Integer, ItemStack> cargo;
    private final long createdAt;
    private final long expiresAt;

    public StoredPackage(
            UUID id,
            StoredPackageKind kind,
            UUID senderId,
            String senderName,
            UUID receiverId,
            String receiverName,
            Map<Integer, ItemStack> cargo,
            long createdAt,
            long expiresAt
    ) {
        this.id = id;
        this.kind = kind;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.cargo = cargo;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID id() {
        return id;
    }

    public StoredPackageKind kind() {
        return kind;
    }

    public UUID senderId() {
        return senderId;
    }

    public String senderName() {
        return senderName;
    }

    public UUID receiverId() {
        return receiverId;
    }

    public String receiverName() {
        return receiverName;
    }

    public Map<Integer, ItemStack> cargo() {
        return cargo;
    }

    public long createdAt() {
        return createdAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

}
