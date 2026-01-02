package me.moontimer.smpcore.chat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import me.moontimer.smpcore.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class IgnoreService {
    private final DatabaseManager database;
    private final Plugin plugin;
    private final ConcurrentMap<UUID, Set<UUID>> ignores = new ConcurrentHashMap<>();

    public IgnoreService(DatabaseManager database, Plugin plugin) {
        this.database = database;
        this.plugin = plugin;
    }

    public void loadIgnores(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<UUID> entries = new HashSet<>();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT ignored_uuid FROM ignores WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        entries.add(UUID.fromString(rs.getString("ignored_uuid")));
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to load ignores", ex);
            }
            Bukkit.getScheduler().runTask(plugin, () -> ignores.put(uuid, entries));
        });
    }

    public void unloadIgnores(UUID uuid) {
        ignores.remove(uuid);
    }

    public boolean isIgnoring(UUID uuid, UUID target) {
        Set<UUID> list = ignores.get(uuid);
        return list != null && list.contains(target);
    }

    public void toggleIgnore(UUID uuid, UUID target, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean nowIgnoring = false;
            try (Connection connection = database.getConnection()) {
                boolean exists = false;
                try (PreparedStatement check = connection.prepareStatement(
                        "SELECT 1 FROM ignores WHERE uuid=? AND ignored_uuid=?")) {
                    check.setString(1, uuid.toString());
                    check.setString(2, target.toString());
                    try (ResultSet rs = check.executeQuery()) {
                        exists = rs.next();
                    }
                }
                if (exists) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM ignores WHERE uuid=? AND ignored_uuid=?")) {
                        delete.setString(1, uuid.toString());
                        delete.setString(2, target.toString());
                        delete.executeUpdate();
                    }
                    nowIgnoring = false;
                } else {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO ignores (uuid, ignored_uuid, created_at) VALUES (?, ?, ?)")) {
                        insert.setString(1, uuid.toString());
                        insert.setString(2, target.toString());
                        insert.setLong(3, System.currentTimeMillis());
                        insert.executeUpdate();
                    }
                    nowIgnoring = true;
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to toggle ignore", ex);
            }
            boolean finalNowIgnoring = nowIgnoring;
            Bukkit.getScheduler().runTask(plugin, () -> {
                ignores.computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet());
                Set<UUID> list = ignores.get(uuid);
                if (list != null) {
                    if (finalNowIgnoring) {
                        list.add(target);
                    } else {
                        list.remove(target);
                    }
                }
                callback.accept(finalNowIgnoring);
            });
        });
    }

    public void listIgnores(UUID uuid, Consumer<List<UUID>> callback) {
        Set<UUID> list = ignores.get(uuid);
        if (list != null) {
            callback.accept(new ArrayList<>(list));
        } else {
            callback.accept(List.of());
        }
    }
}

