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

public class HomeService {
    private final DatabaseManager database;
    private final AuditService audit;
    private final Plugin plugin;

    public HomeService(DatabaseManager database, AuditService audit, Plugin plugin) {
        this.database = database;
        this.audit = audit;
        this.plugin = plugin;
    }

    public void setHome(UUID uuid, String name, Location location, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO homes (uuid, name, world, x, y, z, yaw, pitch, created_at) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                                 + "ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), "
                                 + "z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.setString(3, location.getWorld().getName());
                statement.setDouble(4, location.getX());
                statement.setDouble(5, location.getY());
                statement.setDouble(6, location.getZ());
                statement.setFloat(7, location.getYaw());
                statement.setFloat(8, location.getPitch());
                statement.setLong(9, System.currentTimeMillis());
                success = statement.executeUpdate() > 0;
                audit.log(uuid, "home_set", Map.of("home", name));
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to set home", ex);
            }
            boolean finalSuccess = success;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalSuccess));
        });
    }

    public void deleteHome(UUID uuid, String name, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM homes WHERE uuid=? AND name=?")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                success = statement.executeUpdate() > 0;
                audit.log(uuid, "home_delete", Map.of("home", name));
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete home", ex);
            }
            boolean finalSuccess = success;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalSuccess));
        });
    }

    public void getHome(UUID uuid, String name, Consumer<Location> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location location = null;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM homes WHERE uuid=? AND name=?")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
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
                plugin.getLogger().log(Level.WARNING, "Failed to get home", ex);
            }
            Location finalLocation = location;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalLocation));
        });
    }

    public void listHomes(UUID uuid, Consumer<List<String>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> homes = new ArrayList<>();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT name FROM homes WHERE uuid=? ORDER BY name")) {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        homes.add(rs.getString("name"));
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to list homes", ex);
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(homes));
        });
    }

    public void countHomes(UUID uuid, Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int count = 0;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT COUNT(*) FROM homes WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to count homes", ex);
            }
            int finalCount = count;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalCount));
        });
    }
}

