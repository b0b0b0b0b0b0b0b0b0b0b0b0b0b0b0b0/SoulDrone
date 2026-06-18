package bm.b0b0b0.soulDrone.database;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class MigrationRunner {

    private final HikariDataSource dataSource;

    public MigrationRunner(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS soul_drone_packages (
                        id VARCHAR(36) PRIMARY KEY,
                        kind VARCHAR(16) NOT NULL,
                        sender_uuid VARCHAR(36) NOT NULL,
                        sender_name VARCHAR(16) NOT NULL,
                        receiver_uuid VARCHAR(36) NOT NULL,
                        receiver_name VARCHAR(16) NOT NULL,
                        items_data TEXT NOT NULL,
                        created_at BIGINT NOT NULL,
                        expires_at BIGINT NOT NULL
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to migrate database", exception);
        }
    }

}
