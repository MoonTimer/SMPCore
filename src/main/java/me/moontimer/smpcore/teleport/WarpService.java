package me.moontimer.smpcore.teleport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class WarpService {
    private final DatabaseManager database;
    private final AuditService audit;
    private final Plugin plugin;

    public WarpService(DatabaseManager database, AuditService audit, Plugin plugin) {
        this.database = database;
        this.audit = audit;
        this.plugin = plugin;
    }

    public void setWarp(String name, Location location, UUID actor, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO warps (name, world, x, y, z, yaw, pitch, created_by, created_at) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                                 + "ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), "
                                 + "z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)")) {
                statement.setString(1, name);
                statement.setString(2, location.getWorld().getName());
                statement.setDouble(3, location.getX());
                statement.setDouble(4, location.getY());
                statement.setDouble(5, location.getZ());
                statement.setFloat(6, location.getYaw());
                statement.setFloat(7, location.getPitch());
                statement.setString(8, actor == null ? "console" : actor.toString());
                statement.setLong(9, System.currentTimeMillis());
                success = statement.executeUpdate() > 0;
                audit.log(actor, "warp_set", Map.of("warp", name));
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to set warp", ex);
            }
            boolean finalSuccess = success;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalSuccess));
        });
    }

    public void deleteWarp(String name, UUID actor, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM warps WHERE name=?")) {
                statement.setString(1, name);
                success = statement.executeUpdate() > 0;
                audit.log(actor, "warp_delete", Map.of("warp", name));
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete warp", ex);
            }
            boolean finalSuccess = success;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalSuccess));
        });
    }

    public void getWarp(String name, Consumer<Location> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location location = null;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM warps WHERE name=?")) {
                statement.setString(1, name);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        World world = Bukkit.getWorld(rs.getString("world"));
                        if (world != null) {
                            location = new Location(world, rs.getDouble("x"), rs.getDouble("y"),
                                    rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"));
                        }
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to get warp", ex);
            }
            Location finalLocation = location;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalLocation));
        });
    }

    public void listWarps(Consumer<List<String>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> warps = new ArrayList<>();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT name FROM warps ORDER BY name")) {
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        warps.add(rs.getString("name"));
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to list warps", ex);
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(warps));
        });
    }
}

