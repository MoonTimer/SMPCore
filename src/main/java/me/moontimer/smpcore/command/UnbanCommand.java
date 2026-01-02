package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.moderation.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class UnbanCommand extends BaseCommand {
    private final PunishmentService punishments;

    public UnbanCommand(Plugin plugin, MessageService messages, PunishmentService punishments) {
        super(plugin, messages);
        this.punishments = punishments;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.mod.unban")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/unban <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getUniqueId() == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        punishments.revokePunishment(target.getUniqueId(), PunishmentType.BAN,
                sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                "unban", updated -> {
                    if (updated > 0) {
                        messages.send(sender, "moderation.unban", Map.of("target", args[0]));
                    } else {
                        messages.send(sender, "moderation.no-history");
                    }
                });
        return true;
    }
}

