package bm.b0b0b0.soulDrone.repository;

import bm.b0b0b0.soulDrone.model.StoredPackage;
import bm.b0b0b0.soulDrone.model.StoredPackageKind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PackageRepository {

    CompletableFuture<Void> insert(StoredPackage storedPackage);

    CompletableFuture<Optional<StoredPackage>> findById(UUID id);

    CompletableFuture<List<StoredPackage>> findForPlayer(UUID playerId);

    CompletableFuture<Void> updateItems(UUID id, String itemsData);

    CompletableFuture<Void> delete(UUID id);

    CompletableFuture<Integer> deleteExpired(long nowMillis);

    CompletableFuture<Void> updateKind(UUID id, StoredPackageKind kind, UUID senderId, String senderName, UUID receiverId, String receiverName);

}
