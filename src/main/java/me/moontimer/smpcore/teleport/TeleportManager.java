package me.moontimer.smpcore.teleport;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;
import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.util.DurationFormatter;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TeleportManager {
    private final Plugin plugin;
    private final MessageService messages;
    private final CooldownManager cooldowns;
    private final WarmupManager warmups;
    private final BackManager backManager;
    private final AuditService audit;

    public TeleportManager(Plugin plugin, MessageService messages, CooldownManager cooldowns,
                           WarmupManager warmups, BackManager backManager, AuditService audit) {
        this.plugin = plugin;
        this.messages = messages;
        this.cooldowns = cooldowns;
        this.warmups = warmups;
        this.backManager = backManager;
        this.audit = audit;
    }

    public void requestTeleport(Player player, Location target, TeleportCause cause, TeleportRequest request) {
        FileConfiguration config = plugin.getConfig();
        int warmupSeconds = request.warmupSeconds >= 0
                ? request.warmupSeconds
                : config.getInt("teleport.warmup-seconds", 0);
        boolean cancelOnMove = request.cancelOnMove != null
                ? request.cancelOnMove
                : config.getBoolean("teleport.cancel-on-move", true);
        boolean cancelOnDamage = request.cancelOnDamage != null
                ? request.cancelOnDamage
                : config.getBoolean("teleport.cancel-on-damage", true);

        int cooldownSeconds = request.cooldownSeconds >= 0
                ? request.cooldownSeconds
                : 0;
        String cooldownKey = request.cooldownKey == null ? cause.name().toLowerCase() : request.cooldownKey;

        if (cooldownSeconds > 0 && !hasBypass(player, request.bypassCooldownPermission)) {
            if (cooldowns.isOnCooldown(player.getUniqueId(), cooldownKey)) {
                String remaining = DurationFormatter.formatSeconds(cooldowns.getRemainingMillis(
                        player.getUniqueId(), cooldownKey) / 1000L);
                messages.send(player, "errors.on-cooldown", Map.of("time", remaining));
                return;
            }
        }

        Runnable teleportAction = () -> {
            if (config.getBoolean("back.on-teleport", true) && cause != TeleportCause.BACK) {
                backManager.setBackLocation(player.getUniqueId(), player.getLocation());
            }
            player.teleport(target);
            if (cooldownSeconds > 0 && !hasBypass(player, request.bypassCooldownPermission)) {
                cooldowns.setCooldown(player.getUniqueId(), cooldownKey, cooldownSeconds);
            }
            logTeleport(player.getUniqueId(), cause, target);
            if (request.onComplete != null) {
                request.onComplete.run();
            }
        };

        if (warmupSeconds > 0 && !hasBypass(player, request.bypassWarmupPermission)) {
            messages.send(player, "errors.warmup-start", Map.of("time", String.valueOf(warmupSeconds)));
            IntConsumer onTick = null;
            if (cause == TeleportCause.RTP) {
                onTick = seconds -> sendRtpCountdown(player, seconds);
            }
            Runnable onCancel = () -> {
                messages.send(player, "errors.warmup-cancelled");
                if (cause == TeleportCause.RTP) {
                    sendRtpCancelTitle(player);
                }
            };
            warmups.startWarmup(player, warmupSeconds, cancelOnMove, cancelOnDamage, teleportAction,
                    onCancel, onTick);
        } else {
            teleportAction.run();
        }
    }

    private boolean hasBypass(Player player, String permission) {
        return permission != null && !permission.isEmpty() && player.hasPermission(permission);
    }

    private void logTeleport(UUID uuid, TeleportCause cause, Location target) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cause", cause.name());
        payload.put("world", target.getWorld() == null ? "" : target.getWorld().getName());
        payload.put("x", target.getX());
        payload.put("y", target.getY());
        payload.put("z", target.getZ());
        audit.log(uuid, "teleport", payload);
    }

    private void sendRtpCountdown(Player player, int seconds) {
        String title = messages.format("rtp.countdown-title", Map.of("time", String.valueOf(seconds)));
        String subtitle = messages.format("rtp.countdown-subtitle", Map.of("time", String.valueOf(seconds)));
        player.sendTitle(title, subtitle, 0, 20, 0);
    }

    private void sendRtpCancelTitle(Player player) {
        String title = messages.get("rtp.cancel-title");
        String subtitle = messages.get("rtp.cancel-subtitle");
        player.sendTitle(title, subtitle, 0, 40, 10);
    }

    public record TeleportRequest(
            String cooldownKey,
            int cooldownSeconds,
            int warmupSeconds,
            Boolean cancelOnMove,
            Boolean cancelOnDamage,
            String bypassWarmupPermission,
            String bypassCooldownPermission,
            Runnable onComplete
    ) {
    }
}

