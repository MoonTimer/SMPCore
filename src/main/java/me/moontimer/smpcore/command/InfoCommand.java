package me.moontimer.smpcore.command;

import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.PlayerService;
import me.moontimer.smpcore.menu.StaffMenuService;
import me.moontimer.smpcore.moderation.PunishmentRecord;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.moderation.PunishmentSummary;
import me.moontimer.smpcore.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class InfoCommand extends BaseCommand {
    private final PlayerService playerService;
    private final PunishmentService punishments;
    private final StaffMenuService menus;

    public InfoCommand(Plugin plugin, MessageService messages, PlayerService playerService,
                       PunishmentService punishments, StaffMenuService menus) {
        super(plugin, messages);
        this.playerService = playerService;
        this.punishments = punishments;
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.staff.info")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/info <player>");
            return true;
        }
        String targetName = args[0];
        Player online = Bukkit.getPlayerExact(targetName);
        OfflinePlayer offline = online != null ? online : Bukkit.getOfflinePlayer(targetName);
        if (offline.getUniqueId() == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        String displayName = offline.getName() == null ? targetName : offline.getName();
        if (sender instanceof Player player) {
            menus.openInfoMenu(player, offline.getUniqueId(), displayName, 1);
            return true;
        }
        String onlineIp = online != null && online.getAddress() != null
                ? online.getAddress().getAddress().getHostAddress()
                : "";

        playerService.getPlayerInfo(offline.getUniqueId(), info -> {
            String ip = onlineIp;
            String first = "-";
            String last = "-";
            String playtime = "-";
            if (info != null) {
                if (ip.isEmpty()) {
                    ip = info.ip();
                }
                first = TimeUtil.formatTimestamp(info.firstJoin());
                last = TimeUtil.formatTimestamp(info.lastJoin());
                playtime = formatPlaytime(info.playtimeSeconds());
            }
            String finalIp = ip;
            String finalFirst = first;
            String finalLast = last;
            String finalPlaytime = playtime;

            punishments.getSummary(offline.getUniqueId(), summary -> {
                sendInfo(sender, displayName, offline.getUniqueId().toString(), finalIp,
                        finalFirst, finalLast, finalPlaytime, summary, online);
            });
        });
        return true;
    }

    private void sendInfo(CommandSender sender, String name, String uuid, String ip,
                          String first, String last, String playtime, PunishmentSummary summary, Player online) {
        messages.send(sender, "staff.info-header", Map.of("player", name));
        messages.send(sender, "staff.info-line", Map.of("uuid", uuid, "ip", ip == null ? "" : ip));
        messages.send(sender, "staff.info-joins", Map.of("first", first, "last", last));
        messages.send(sender, "staff.info-playtime", Map.of("playtime", playtime));
        messages.send(sender, "staff.info-warnings", Map.of("warnings", String.valueOf(summary.warnings())));

        sendLastRecord(sender, summary.lastBan(), "staff.info-lastban", "staff.info-lastban-none");
        sendLastRecord(sender, summary.lastMute(), "staff.info-lastmute", "staff.info-lastmute-none");
        sendLastWarn(sender, summary.lastWarn());

        if (sender instanceof Player player && online != null && player.hasPermission("smpcore.staff.vtp")) {
            Component prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(messages.getRaw("prefix"));
            Component button = LegacyComponentSerializer.legacyAmpersand().deserialize(messages.getRaw("staff.info-teleport"))
                    .clickEvent(ClickEvent.runCommand("/vtp " + online.getName()))
                    .hoverEvent(HoverEvent.showText(Component.text("Vanish + TP")));
            player.sendMessage(prefix.append(button));
        }
    }

    private void sendLastRecord(CommandSender sender, PunishmentRecord record, String key, String noneKey) {
        if (record == null) {
            messages.send(sender, noneKey);
            return;
        }
        String status = getStatus(record);
        messages.send(sender, key, Map.of(
                "reason", record.reason(),
                "status", status,
                "created", TimeUtil.formatTimestamp(record.createdAt())
        ));
    }

    private void sendLastWarn(CommandSender sender, PunishmentRecord record) {
        if (record == null) {
            messages.send(sender, "staff.info-lastwarn-none");
            return;
        }
        messages.send(sender, "staff.info-lastwarn", Map.of(
                "reason", record.reason(),
                "created", TimeUtil.formatTimestamp(record.createdAt())
        ));
    }

    private String getStatus(PunishmentRecord record) {
        if (record.revokedAt() != null) {
            return "widerrufen";
        }
        if (record.expiresAt() != null && record.expiresAt() <= System.currentTimeMillis()) {
            return "abgelaufen";
        }
        return "aktiv";
    }

    private String formatPlaytime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format(Locale.ROOT, "%dh %dm", hours, minutes);
    }
}
