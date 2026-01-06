package me.moontimer.smpcore.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        FileConfiguration config = plugin.getConfig();
        String host = config.getString("mysql.host", "localhost");
        int port = config.getInt("mysql.port", 3306);
        String database = config.getString("mysql.database", "smpcore");
        String user = config.getString("mysql.user", "root");
        String password = config.getString("mysql.password", "");
        boolean useSsl = config.getBoolean("mysql.use-ssl", false);
        int maxPool = config.getInt("mysql.pool.maximum-pool-size", 10);
        int minIdle = config.getInt("mysql.pool.minimum-idle", 2);

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=" + useSsl + "&allowPublicKeyRetrieval=true&characterEncoding=utf8";

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(maxPool);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setPoolName("smpcore-pool");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            try (Connection connection = dataSource.getConnection()) {
                return true;
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL", ex);
            return false;
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void migrate() {
        List<String> statements = new ArrayList<>();
        statements.add("CREATE TABLE IF NOT EXISTS players ("
                + "uuid VARCHAR(36) PRIMARY KEY,"
                + "name_last VARCHAR(16) NOT NULL,"
                + "ip_last VARCHAR(64) NOT NULL,"
                + "first_join BIGINT NOT NULL,"
                + "last_join BIGINT NOT NULL,"
                + "playtime_seconds BIGINT NOT NULL"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS homes ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "uuid VARCHAR(36) NOT NULL,"
                + "name VARCHAR(32) NOT NULL,"
                + "world VARCHAR(64) NOT NULL,"
                + "x DOUBLE NOT NULL,"
                + "y DOUBLE NOT NULL,"
                + "z DOUBLE NOT NULL,"
                + "yaw FLOAT NOT NULL,"
                + "pitch FLOAT NOT NULL,"
                + "created_at BIGINT NOT NULL,"
                + "UNIQUE KEY homes_owner_name (uuid, name)"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS warps ("
                + "name VARCHAR(32) PRIMARY KEY,"
                + "world VARCHAR(64) NOT NULL,"
                + "x DOUBLE NOT NULL,"
                + "y DOUBLE NOT NULL,"
                + "z DOUBLE NOT NULL,"
                + "yaw FLOAT NOT NULL,"
                + "pitch FLOAT NOT NULL,"
                + "created_by VARCHAR(36) NOT NULL,"
                + "created_at BIGINT NOT NULL"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS server_locations ("
                + "key_name VARCHAR(64) PRIMARY KEY,"
                + "world VARCHAR(64) NOT NULL,"
                + "x DOUBLE NOT NULL,"
                + "y DOUBLE NOT NULL,"
                + "z DOUBLE NOT NULL,"
                + "yaw FLOAT NOT NULL,"
                + "pitch FLOAT NOT NULL"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS ignores ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "ignored_uuid VARCHAR(36) NOT NULL,"
                + "created_at BIGINT NOT NULL,"
                + "UNIQUE KEY ignores_pair (uuid, ignored_uuid)"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS punishments ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "type VARCHAR(16) NOT NULL,"
                + "target_uuid VARCHAR(36) NOT NULL,"
                + "target_name_snapshot VARCHAR(16) NOT NULL,"
                + "target_ip_snapshot VARCHAR(64),"
                + "actor_uuid VARCHAR(36),"
                + "actor_name_snapshot VARCHAR(16),"
                + "reason VARCHAR(255) NOT NULL,"
                + "created_at BIGINT NOT NULL,"
                + "expires_at BIGINT,"
                + "revoked_at BIGINT,"
                + "revoked_by_uuid VARCHAR(36),"
                + "revoked_reason VARCHAR(255),"
                + "scope VARCHAR(32) NOT NULL,"
                + "KEY punish_target_type_created (target_uuid, type, created_at),"
                + "KEY punish_expires_revoked (expires_at, revoked_at),"
                + "KEY punish_actor_created (actor_uuid, created_at)"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS templates ("
                + "key_name VARCHAR(32) PRIMARY KEY,"
                + "type VARCHAR(16) NOT NULL,"
                + "default_reason VARCHAR(255) NOT NULL,"
                + "durations TEXT,"
                + "ladder_mode VARCHAR(16) NOT NULL,"
                + "silent_default BOOLEAN NOT NULL"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS audit_log ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "uuid VARCHAR(36),"
                + "action VARCHAR(64) NOT NULL,"
                + "payload TEXT,"
                + "created_at BIGINT NOT NULL"
                + ")");

        statements.add("CREATE TABLE IF NOT EXISTS auction_listings ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "seller_uuid VARCHAR(36),"
                + "seller_name VARCHAR(16) NOT NULL,"
                + "item_data LONGTEXT NOT NULL,"
                + "price DOUBLE NOT NULL,"
                + "created_at BIGINT NOT NULL,"
                + "expires_at BIGINT,"
                + "status VARCHAR(16) NOT NULL,"
                + "buyer_uuid VARCHAR(36),"
                + "buyer_name VARCHAR(16),"
                + "sold_at BIGINT,"
                + "server_listing BOOLEAN NOT NULL,"
                + "unlimited BOOLEAN NOT NULL,"
                + "stock INT NOT NULL,"
                + "search_text TEXT,"
                + "sold_cleared BOOLEAN NOT NULL DEFAULT FALSE,"
                + "updated_at BIGINT NOT NULL,"
                + "KEY ah_status_created (status, created_at),"
                + "KEY ah_seller_status (seller_uuid, status)"
                + ")");

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Database migration failed", ex);
        }
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}

