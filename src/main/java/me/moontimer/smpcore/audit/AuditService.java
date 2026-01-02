package me.moontimer.smpcore.audit;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import me.moontimer.smpcore.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class AuditService {
    private final Plugin plugin;
    private final DatabaseManager database;
    private final Gson gson = new Gson();

    public AuditService(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void log(UUID uuid, String action, Map<String, Object> payload) {
        long now = System.currentTimeMillis();
        String json = payload == null ? null : gson.toJson(payload);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO audit_log (uuid, action, payload, created_at) VALUES (?, ?, ?, ?)") ) {
                statement.setString(1, uuid == null ? null : uuid.toString());
                statement.setString(2, action);
                statement.setString(3, json);
                statement.setLong(4, now);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to insert audit log", ex);
            }
            writeFileLog(now, uuid, action, json);
        });
    }

    private void writeFileLog(long timestamp, UUID uuid, String action, String json) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("audit.file-enabled", false)) {
            return;
        }
        String pathValue = config.getString("audit.file-path", "logs/smpcore-audit.log");
        if (pathValue == null || pathValue.isEmpty()) {
            return;
        }
        Path path = plugin.getDataFolder().toPath().resolve(pathValue);
        String line = timestamp + "|" + (uuid == null ? "" : uuid) + "|" + action + "|" + (json == null ? "{}" : json)
                + System.lineSeparator();
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to write audit log file", ex);
        }
    }
}

