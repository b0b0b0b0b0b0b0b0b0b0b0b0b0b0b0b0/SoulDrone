package bm.b0b0b0.soulDrone.repository;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.database.DatabaseBootstrap;
import bm.b0b0b0.soulDrone.model.StoredPackage;
import bm.b0b0b0.soulDrone.model.StoredPackageKind;
import bm.b0b0b0.soulDrone.util.ItemStackSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SqlPackageRepository implements PackageRepository {

    private final DatabaseBootstrap database;
    private final List<Integer> cargoSlots;

    public SqlPackageRepository(DatabaseBootstrap database, PluginConfig config) {
        this.database = database;
        this.cargoSlots = config.sortedCargoSlots();
    }

    @Override
    public CompletableFuture<Void> insert(StoredPackage storedPackage) {
        return database.runAsync(() -> {
            try (Connection connection = database.connection();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO soul_drone_packages
                         (id, kind, sender_uuid, sender_name, receiver_uuid, receiver_name, items_data, created_at, expires_at)
                         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                         """)) {
                statement.setString(1, storedPackage.id().toString());
                statement.setString(2, storedPackage.kind().name());
                statement.setString(3, storedPackage.senderId().toString());
                statement.setString(4, storedPackage.senderName());
                statement.setString(5, storedPackage.receiverId().toString());
                statement.setString(6, storedPackage.receiverName());
                statement.setString(7, ItemStackSerializer.serializeCargo(storedPackage.cargo()));
                statement.setLong(8, storedPackage.createdAt());
                statement.setLong(9, storedPackage.expiresAt());
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to insert package", exception);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Optional<StoredPackage>> findById(UUID id) {
        return database.runAsync(() -> {
            try (Connection connection = database.connection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT id, kind, sender_uuid, sender_name, receiver_uuid, receiver_name, items_data, created_at, expires_at
                         FROM soul_drone_packages WHERE id = ?
                         """)) {
                statement.setString(1, id.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(mapRow(resultSet));
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to find package", exception);
            }
        });
    }

    @Override
    public CompletableFuture<List<StoredPackage>> findForPlayer(UUID playerId) {
        return database.runAsync(() -> {
            try (Connection connection = database.connection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT id, kind, sender_uuid, sender_name, receiver_uuid, receiver_name, items_data, created_at, expires_at
                         FROM soul_drone_packages
                         WHERE sender_uuid = ? OR receiver_uuid = ?
                         ORDER BY created_at ASC
                         """)) {
                String id = playerId.toString();
                statement.setString(1, id);
                statement.setString(2, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<StoredPackage> packages = new ArrayList<>();
                    while (resultSet.next()) {
                        packages.add(mapRow(resultSet));
                    }
                    return packages;
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to list packages", exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateItems(UUID id, String itemsData) {
        return database.runAsync(() -> {
            try (Connection connection = database.connection();
                 PreparedStatement statement = connection.prepareStatement("""
                         UPDATE soul_drone_packages SET items_data = ? WHERE id = ?
                         """)) {
                statement.setString(1, itemsData);
                statement.setString(2, id.toString());
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to update package items", exception);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> delete(UUID id) {
        return database.runAsync(() -> {
            try (Connection connection = database.connection();
                 PreparedStatement statement = connection.prepareStatement("""
                         DELETE FROM soul_drone_packages WHERE id = ?
                         """)) {
                statement.setString(1, id.toString());
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to delete package", exception);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Integer> deleteExpired(long nowMillis) {
        return database.runAsync(() -> {
            try (Connection connection = database.connection();
                 PreparedStatement statement = connection.prepareStatement("""
                         DELETE FROM soul_drone_packages WHERE expires_at <= ?
                         """)) {
                statement.setLong(1, nowMillis);
                return statement.executeUpdate();
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to delete expired packages", exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateKind(
            UUID id,
            StoredPackageKind kind,
            UUID senderId,
            String senderName,
            UUID receiverId,
            String receiverName
    ) {
        return database.runAsync(() -> {
            try (Connection connection = database.connection();
                 PreparedStatement statement = connection.prepareStatement("""
                         UPDATE soul_drone_packages
                         SET kind = ?, sender_uuid = ?, sender_name = ?, receiver_uuid = ?, receiver_name = ?
                         WHERE id = ?
                         """)) {
                statement.setString(1, kind.name());
                statement.setString(2, senderId.toString());
                statement.setString(3, senderName);
                statement.setString(4, receiverId.toString());
                statement.setString(5, receiverName);
                statement.setString(6, id.toString());
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to update package kind", exception);
            }
            return null;
        });
    }

    private StoredPackage mapRow(ResultSet resultSet) throws Exception {
        Map<Integer, ItemStack> cargo = ItemStackSerializer.deserializeCargo(
                resultSet.getString("items_data"),
                cargoSlots
        );
        return new StoredPackage(
                UUID.fromString(resultSet.getString("id")),
                StoredPackageKind.valueOf(resultSet.getString("kind")),
                UUID.fromString(resultSet.getString("sender_uuid")),
                resultSet.getString("sender_name"),
                UUID.fromString(resultSet.getString("receiver_uuid")),
                resultSet.getString("receiver_name"),
                cargo,
                resultSet.getLong("created_at"),
                resultSet.getLong("expires_at")
        );
    }

}
