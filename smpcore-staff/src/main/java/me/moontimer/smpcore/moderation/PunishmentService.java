package me.moontimer.smpcore.moderation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PunishmentService {
    private final Plugin plugin;
    private final DatabaseManager database;
    private final AuditService audit;
    private final MessageService messages;
    private final Map<UUID, PunishmentRecord> activeMutes = new ConcurrentHashMap<>();

    public PunishmentService(Plugin plugin, DatabaseManager database, AuditService audit, MessageService messages) {
        this.plugin = plugin;
        this.database = database;
        this.audit = audit;
        this.messages = messages;
    }

    public void createPunishment(PunishmentType type, OfflinePlayer target, String targetIp, UUID actorUuid,
                                 String actorName, String reason, Long expiresAt, Consumer<Long> callback) {
        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO punishments (type, target_uuid, target_name_snapshot, target_ip_snapshot, "
                                 + "actor_uuid, actor_name_snapshot, reason, created_at, expires_at, revoked_at, "
                                 + "revoked_by_uuid, revoked_reason, scope) "
                                 + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, type.name());
                statement.setString(2, target.getUniqueId().toString());
                statement.setString(3, target.getName() == null ? "unknown" : target.getName());
                statement.setString(4, targetIp);
                statement.setString(5, actorUuid == null ? null : actorUuid.toString());
                statement.setString(6, actorName);
                statement.setString(7, reason);
                statement.setLong(8, now);
                if (expiresAt == null) {
                    statement.setNull(9, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(9, expiresAt);
                }
                statement.setString(10, "GLOBAL");
                statement.executeUpdate();

                long id = -1;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        id = keys.getLong(1);
                    }
                }
                long finalId = id;
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalId));

                if (type == PunishmentType.MUTE) {
                    refreshMute(target.getUniqueId());
                }
                audit.log(actorUuid, "punishment_create", Map.of(
                        "type", type.name(),
                        "target", target.getUniqueId().toString(),
                        "reason", reason,
                        "expires", expiresAt == null ? 0 : expiresAt
                ));
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to create punishment", ex);
            }
        });
    }

    public void revokePunishment(UUID target, PunishmentType type, UUID actorUuid, String reason,
                                 Consumer<Integer> callback) {
        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE punishments SET revoked_at=?, revoked_by_uuid=?, revoked_reason=? "
                                 + "WHERE target_uuid=? AND type=? AND revoked_at IS NULL AND "
                                 + "(expires_at IS NULL OR expires_at > ?)")) {
                statement.setLong(1, now);
                statement.setString(2, actorUuid == null ? null : actorUuid.toString());
                statement.setString(3, reason);
                statement.setString(4, target.toString());
                statement.setString(5, type.name());
                statement.setLong(6, now);
                int updated = statement.executeUpdate();
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(updated));

                if (type == PunishmentType.MUTE) {
                    activeMutes.remove(target);
                }
                audit.log(actorUuid, "punishment_revoke", Map.of(
                        "type", type.name(),
                        "target", target.toString(),
                        "reason", reason
                ));
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to revoke punishment", ex);
            }
        });
    }

    public void revokeLatestWarn(UUID target, UUID actorUuid, String reason, Consumer<Boolean> callback) {
        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean updated = false;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE punishments SET revoked_at=?, revoked_by_uuid=?, revoked_reason=? WHERE id=("
                                 + "SELECT id FROM (SELECT id FROM punishments WHERE target_uuid=? AND type=? "
                                 + "AND revoked_at IS NULL ORDER BY created_at DESC LIMIT 1) AS t)")) {
                statement.setLong(1, now);
                statement.setString(2, actorUuid == null ? null : actorUuid.toString());
                statement.setString(3, reason);
                statement.setString(4, target.toString());
                statement.setString(5, PunishmentType.WARN.name());
                updated = statement.executeUpdate() > 0;
                audit.log(actorUuid, "punishment_revoke_latest_warn", Map.of(
                        "target", target.toString(),
                        "reason", reason
                ));
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to revoke latest warning", ex);
            }
            boolean finalUpdated = updated;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalUpdated));
        });
    }

    public void clearWarnings(UUID target, UUID actorUuid, String reason, Consumer<Integer> callback) {
        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int updated = 0;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE punishments SET revoked_at=?, revoked_by_uuid=?, revoked_reason=? "
                                 + "WHERE target_uuid=? AND type=? AND revoked_at IS NULL")) {
                statement.setLong(1, now);
                statement.setString(2, actorUuid == null ? null : actorUuid.toString());
                statement.setString(3, reason);
                statement.setString(4, target.toString());
                statement.setString(5, PunishmentType.WARN.name());
                updated = statement.executeUpdate();
                audit.log(actorUuid, "punishment_clear_warnings", Map.of(
                        "target", target.toString(),
                        "count", updated
                ));
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to clear warnings", ex);
            }
            int finalUpdated = updated;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalUpdated));
        });
    }

    public PunishmentRecord findActiveBanSync(UUID uuid, String ip) {
        long now = System.currentTimeMillis();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM punishments WHERE type=? AND revoked_at IS NULL AND "
                             + "(expires_at IS NULL OR expires_at > ?) AND (target_uuid=? OR target_ip_snapshot=?) "
                             + "ORDER BY created_at DESC LIMIT 1")) {
            statement.setString(1, PunishmentType.BAN.name());
            statement.setLong(2, now);
            statement.setString(3, uuid.toString());
            statement.setString(4, ip);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to query active ban", ex);
        }
        return null;
    }

    public void getActiveBan(UUID uuid, String ip, Consumer<PunishmentRecord> callback) {
        String safeIp = ip == null ? "" : ip;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PunishmentRecord record = null;
            long now = System.currentTimeMillis();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM punishments WHERE type=? AND revoked_at IS NULL AND "
                                 + "(expires_at IS NULL OR expires_at > ?) AND (target_uuid=? OR target_ip_snapshot=?) "
                                 + "ORDER BY created_at DESC LIMIT 1")) {
                statement.setString(1, PunishmentType.BAN.name());
                statement.setLong(2, now);
                statement.setString(3, uuid.toString());
                statement.setString(4, safeIp);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        record = mapRecord(rs);
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to query active ban", ex);
            }
            PunishmentRecord finalRecord = record;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalRecord));
        });
    }

    public void getActiveMute(UUID uuid, Consumer<PunishmentRecord> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PunishmentRecord record = null;
            long now = System.currentTimeMillis();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM punishments WHERE type=? AND revoked_at IS NULL AND "
                                 + "(expires_at IS NULL OR expires_at > ?) AND target_uuid=? "
                                 + "ORDER BY created_at DESC LIMIT 1")) {
                statement.setString(1, PunishmentType.MUTE.name());
                statement.setLong(2, now);
                statement.setString(3, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        record = mapRecord(rs);
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to query active mute", ex);
            }
            PunishmentRecord finalRecord = record;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalRecord));
        });
    }

    public void refreshMute(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PunishmentRecord record = null;
            long now = System.currentTimeMillis();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM punishments WHERE type=? AND revoked_at IS NULL AND "
                                 + "(expires_at IS NULL OR expires_at > ?) AND target_uuid=? "
                                 + "ORDER BY created_at DESC LIMIT 1")) {
                statement.setString(1, PunishmentType.MUTE.name());
                statement.setLong(2, now);
                statement.setString(3, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        record = mapRecord(rs);
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to query active mute", ex);
            }
            PunishmentRecord finalRecord = record;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalRecord == null) {
                    activeMutes.remove(uuid);
                } else {
                    activeMutes.put(uuid, finalRecord);
                }
            });
        });
    }

    public PunishmentRecord getActiveMute(UUID uuid) {
        PunishmentRecord record = activeMutes.get(uuid);
        if (record == null) {
            return null;
        }
        if (!record.isActive(System.currentTimeMillis())) {
            activeMutes.remove(uuid);
            return null;
        }
        return record;
    }

    public void getHistory(UUID target, int page, int pageSize, BiConsumer<List<PunishmentRecord>, Integer> callback) {
        fetchHistory("target_uuid", target.toString(), page, pageSize, callback);
    }

    public void getStaffHistory(UUID staff, int page, int pageSize, BiConsumer<List<PunishmentRecord>, Integer> callback) {
        fetchHistory("actor_uuid", staff.toString(), page, pageSize, callback);
    }

    private void fetchHistory(String column, String value, int page, int pageSize,
                              BiConsumer<List<PunishmentRecord>, Integer> callback) {
        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * pageSize;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PunishmentRecord> records = new ArrayList<>();
            int total = 0;
            try (Connection connection = database.getConnection()) {
                try (PreparedStatement countStatement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM punishments WHERE " + column + "=?")) {
                    countStatement.setString(1, value);
                    try (ResultSet count = countStatement.executeQuery()) {
                        if (count.next()) {
                            total = count.getInt(1);
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM punishments WHERE " + column + "=? ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
                    statement.setString(1, value);
                    statement.setInt(2, pageSize);
                    statement.setInt(3, offset);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            records.add(mapRecord(rs));
                        }
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to query punishment history", ex);
            }
            int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(records, totalPages));
        });
    }

    public void getBanList(int page, int pageSize, BiConsumer<List<PunishmentRecord>, Integer> callback) {
        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * pageSize;
        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PunishmentRecord> records = new ArrayList<>();
            int total = 0;
            try (Connection connection = database.getConnection()) {
                try (PreparedStatement countStatement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM punishments WHERE type=? AND revoked_at IS NULL AND "
                                + "(expires_at IS NULL OR expires_at > ?)") ) {
                    countStatement.setString(1, PunishmentType.BAN.name());
                    countStatement.setLong(2, now);
                    try (ResultSet count = countStatement.executeQuery()) {
                        if (count.next()) {
                            total = count.getInt(1);
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM punishments WHERE type=? AND revoked_at IS NULL AND "
                                + "(expires_at IS NULL OR expires_at > ?) ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
                    statement.setString(1, PunishmentType.BAN.name());
                    statement.setLong(2, now);
                    statement.setInt(3, pageSize);
                    statement.setInt(4, offset);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            records.add(mapRecord(rs));
                        }
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to query ban list", ex);
            }
            int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(records, totalPages));
        });
    }

    public void getWarnings(UUID target, int page, int pageSize, BiConsumer<List<PunishmentRecord>, Integer> callback) {
        fetchHistoryByType(target, PunishmentType.WARN, page, pageSize, callback);
    }

    public void getSummary(UUID target, Consumer<PunishmentSummary> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int warnings = 0;
            PunishmentRecord lastBan = null;
            PunishmentRecord lastMute = null;
            PunishmentRecord lastWarn = null;
            try (Connection connection = database.getConnection()) {
                try (PreparedStatement countStatement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM punishments WHERE target_uuid=? AND type=?")) {
                    countStatement.setString(1, target.toString());
                    countStatement.setString(2, PunishmentType.WARN.name());
                    try (ResultSet count = countStatement.executeQuery()) {
                        if (count.next()) {
                            warnings = count.getInt(1);
                        }
                    }
                }
                lastBan = fetchLastOfType(connection, target, PunishmentType.BAN);
                lastMute = fetchLastOfType(connection, target, PunishmentType.MUTE);
                lastWarn = fetchLastOfType(connection, target, PunishmentType.WARN);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to query punishment summary", ex);
            }
            PunishmentSummary summary = new PunishmentSummary(warnings, lastBan, lastMute, lastWarn);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(summary));
        });
    }

    private PunishmentRecord mapRecord(ResultSet rs) throws SQLException {
        return new PunishmentRecord(
                rs.getLong("id"),
                PunishmentType.valueOf(rs.getString("type")),
                rs.getString("target_uuid"),
                rs.getString("target_name_snapshot"),
                rs.getString("actor_uuid"),
                rs.getString("actor_name_snapshot"),
                rs.getString("reason"),
                rs.getLong("created_at"),
                getNullableLong(rs, "expires_at"),
                getNullableLong(rs, "revoked_at")
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private void fetchHistoryByType(UUID target, PunishmentType type, int page, int pageSize,
                                    BiConsumer<List<PunishmentRecord>, Integer> callback) {
        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * pageSize;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PunishmentRecord> records = new ArrayList<>();
            int total = 0;
            try (Connection connection = database.getConnection()) {
                try (PreparedStatement countStatement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM punishments WHERE target_uuid=? AND type=?")) {
                    countStatement.setString(1, target.toString());
                    countStatement.setString(2, type.name());
                    try (ResultSet count = countStatement.executeQuery()) {
                        if (count.next()) {
                            total = count.getInt(1);
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM punishments WHERE target_uuid=? AND type=? "
                                + "ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
                    statement.setString(1, target.toString());
                    statement.setString(2, type.name());
                    statement.setInt(3, pageSize);
                    statement.setInt(4, offset);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            records.add(mapRecord(rs));
                        }
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to query punishment history", ex);
            }
            int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(records, totalPages));
        });
    }

    private PunishmentRecord fetchLastOfType(Connection connection, UUID target, PunishmentType type) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM punishments WHERE target_uuid=? AND type=? ORDER BY created_at DESC LIMIT 1")) {
            statement.setString(1, target.toString());
            statement.setString(2, type.name());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }
        return null;
    }

    public String getBanScreen(PunishmentRecord record) {
        return buildBanScreen(record.reason(), record.expiresAt(), record.id());
    }

    public String buildBanScreen(String reason, Long expiresAt, long id) {
        FileConfiguration config = plugin.getConfig();
        String template = config.getString("punishments.ban-screen", "You are banned: {reason}");
        String expires = expiresAt == null ? "never" : me.moontimer.smpcore.util.TimeUtil.formatTimestamp(expiresAt);
        String message = template
                .replace("{reason}", reason)
                .replace("{expires}", expires)
                .replace("{id}", String.valueOf(id));
        return messages.colorize(message);
    }
}

