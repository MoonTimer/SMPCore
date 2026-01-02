package me.moontimer.smpcore.core;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.function.Consumer;
import me.moontimer.smpcore.db.DatabaseManager;
import me.moontimer.smpcore.model.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerService {
    private final Plugin plugin;
    private final DatabaseManager database;
    private final Map<UUID, Long> loginTimes = new ConcurrentHashMap<>();

    public PlayerService(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void handleJoin(Player player) {
        long now = System.currentTimeMillis();
        loginTimes.put(player.getUniqueId(), now);
        String name = player.getName();
        String ip = getIp(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO players (uuid, name_last, ip_last, first_join, last_join, playtime_seconds) "
                                 + "VALUES (?, ?, ?, ?, ?, ?) "
                                 + "ON DUPLICATE KEY UPDATE name_last=VALUES(name_last), ip_last=VALUES(ip_last), "
                                 + "last_join=VALUES(last_join)")) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, name);
                statement.setString(3, ip);
                statement.setLong(4, now);
                statement.setLong(5, now);
                statement.setLong(6, 0L);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to update player join", ex);
            }
        });
    }

    public void handleQuit(Player player) {
        Long login = loginTimes.remove(player.getUniqueId());
        if (login == null) {
            return;
        }
        long sessionSeconds = Math.max(0L, (System.currentTimeMillis() - login) / 1000L);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection()) {
                long current = 0L;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT playtime_seconds FROM players WHERE uuid=?")) {
                    select.setString(1, player.getUniqueId().toString());
                    try (ResultSet rs = select.executeQuery()) {
                        if (rs.next()) {
                            current = rs.getLong(1);
                        }
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE players SET playtime_seconds=?, last_join=? WHERE uuid=?")) {
                    update.setLong(1, current + sessionSeconds);
                    update.setLong(2, System.currentTimeMillis());
                    update.setString(3, player.getUniqueId().toString());
                    update.executeUpdate();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to update player quit", ex);
            }
        });
    }

    private String getIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) {
            return "";
        }
        return address.getAddress().getHostAddress();
    }

    public void getPlayerInfo(UUID uuid, Consumer<PlayerInfo> callback) {
        fetchPlayerInfo("uuid", uuid.toString(), callback);
    }

    public void getPlayerInfoByName(String name, Consumer<PlayerInfo> callback) {
        fetchPlayerInfo("name_last", name, callback);
    }

    private void fetchPlayerInfo(String column, String value, Consumer<PlayerInfo> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerInfo info = null;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM players WHERE " + column + "=?")) {
                statement.setString(1, value);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        info = new PlayerInfo(
                                rs.getString("uuid"),
                                rs.getString("name_last"),
                                rs.getString("ip_last"),
                                rs.getLong("first_join"),
                                rs.getLong("last_join"),
                                rs.getLong("playtime_seconds")
                        );
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to fetch player info", ex);
            }
            PlayerInfo finalInfo = info;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalInfo));
        });
    }
}

