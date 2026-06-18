package bm.b0b0b0.soulDrone.model;

import java.util.UUID;

public final class DeliveryRequest {

    private final UUID requestId;
    private final UUID senderId;
    private final UUID receiverId;
    private final String senderName;
    private final String receiverName;

    public DeliveryRequest(UUID senderId, String senderName, UUID receiverId, String receiverName) {
        this.requestId = UUID.randomUUID();
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
    }

    public UUID requestId() {
        return requestId;
    }

    public UUID senderId() {
        return senderId;
    }

    public UUID receiverId() {
        return receiverId;
    }

    public String senderName() {
        return senderName;
    }

    public String receiverName() {
        return receiverName;
    }

}
