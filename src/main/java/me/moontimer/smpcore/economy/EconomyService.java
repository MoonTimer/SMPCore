package me.moontimer.smpcore.economy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class EconomyService {
    private final DatabaseManager database;
    private final AuditService audit;
    private final Plugin plugin;

    public EconomyService(DatabaseManager database, AuditService audit, Plugin plugin) {
        this.database = database;
        this.audit = audit;
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("economy.enabled", false);
    }

    public double getStartBalance() {
        return plugin.getConfig().getDouble("economy.start-balance", 0.0);
    }

    public void getBalance(UUID uuid, Consumer<Double> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = ensureAccount(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(balance));
        });
    }

    public void transfer(UUID from, UUID to, double amount, Consumer<TransferResult> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TransferResult result = doTransfer(from, to, amount);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    private double ensureAccount(UUID uuid) {
        double start = getStartBalance();
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            double balance = start;
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT balance FROM economy_balances WHERE uuid=?")) {
                select.setString(1, uuid.toString());
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        balance = rs.getDouble("balance");
                    } else {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO economy_balances (uuid, balance, updated_at) VALUES (?, ?, ?)") ) {
                            insert.setString(1, uuid.toString());
                            insert.setDouble(2, start);
                            insert.setLong(3, System.currentTimeMillis());
                            insert.executeUpdate();
                        }
                    }
                }
            }
            connection.commit();
            return balance;
        } catch (SQLException ex) {
            return start;
        }
    }

    private TransferResult doTransfer(UUID from, UUID to, double amount) {
        double start = getStartBalance();
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            double fromBalance = getBalanceInternal(connection, from, start);
            if (fromBalance < amount) {
                connection.rollback();
                return new TransferResult(false, fromBalance);
            }
            double toBalance = getBalanceInternal(connection, to, start);
            updateBalance(connection, from, fromBalance - amount);
            updateBalance(connection, to, toBalance + amount);
            connection.commit();
            audit.log(from, "economy_transfer", Map.of(
                    "to", to.toString(),
                    "amount", amount
            ));
            return new TransferResult(true, fromBalance - amount);
        } catch (SQLException ex) {
            return new TransferResult(false, 0.0);
        }
    }

    private double getBalanceInternal(Connection connection, UUID uuid, double start) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT balance FROM economy_balances WHERE uuid=?")) {
            select.setString(1, uuid.toString());
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO economy_balances (uuid, balance, updated_at) VALUES (?, ?, ?)") ) {
            insert.setString(1, uuid.toString());
            insert.setDouble(2, start);
            insert.setLong(3, System.currentTimeMillis());
            insert.executeUpdate();
        }
        return start;
    }

    private void updateBalance(Connection connection, UUID uuid, double balance) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE economy_balances SET balance=?, updated_at=? WHERE uuid=?")) {
            update.setDouble(1, balance);
            update.setLong(2, System.currentTimeMillis());
            update.setString(3, uuid.toString());
            update.executeUpdate();
        }
    }

    public record TransferResult(boolean success, double newBalance) {
    }
}

