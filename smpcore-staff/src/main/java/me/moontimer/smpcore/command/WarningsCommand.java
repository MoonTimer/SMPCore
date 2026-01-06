package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.moderation.PunishmentRecord;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.util.DurationFormatter;
import me.moontimer.smpcore.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class WarningsCommand extends BaseCommand {
    private final PunishmentService punishments;

    public WarningsCommand(Plugin plugin, MessageService messages, PunishmentService punishments) {
        super(plugin, messages);
        this.punishments = punishments;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.mod.history")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/warnings <player> [page]");
            return true;
        }
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        int finalPage = page;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getUniqueId() == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        int pageSize = 10;
        punishments.getWarnings(target.getUniqueId(), finalPage, pageSize, (records, totalPages) -> {
            if (records.isEmpty()) {
                messages.send(sender, "moderation.no-history");
                return;
            }
            messages.send(sender, "moderation.warnings-header", Map.of(
                    "target", args[0],
                    "page", String.valueOf(finalPage),
                    "pages", String.valueOf(totalPages)
            ));
            for (PunishmentRecord record : records) {
                sender.sendMessage(formatHistoryLine(record));
            }
        });
        return true;
    }

    private String formatHistoryLine(PunishmentRecord record) {
        String expires;
        if (record.expiresAt() == null) {
            expires = "permanent";
        } else {
            long remaining = Math.max(0L, (record.expiresAt() - System.currentTimeMillis()) / 1000L);
            expires = DurationFormatter.formatSeconds(remaining);
        }
        return messages.get("prefix") + messages.format("moderation.history-line", Map.of(
                "id", String.valueOf(record.id()),
                "type", record.type().name(),
                "reason", record.reason(),
                "created", TimeUtil.formatTimestamp(record.createdAt()),
                "expires", expires
        ));
    }
}
