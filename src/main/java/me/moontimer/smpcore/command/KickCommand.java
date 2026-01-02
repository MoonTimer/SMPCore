package me.moontimer.smpcore.command;

import java.util.Map;
import java.util.StringJoiner;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.moderation.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class KickCommand extends BaseCommand {
    private final PunishmentService punishments;

    public KickCommand(Plugin plugin, MessageService messages, PunishmentService punishments) {
        super(plugin, messages);
        this.punishments = punishments;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.mod.kick")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/kick <player> [reason]");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        String reason;
        if (args.length > 1) {
            StringJoiner joiner = new StringJoiner(" ");
            for (int i = 1; i < args.length; i++) {
                joiner.add(args[i]);
            }
            reason = joiner.toString();
        } else {
            reason = plugin.getConfig().getString("punishments.default-reason", "Misconduct");
        }

        punishments.createPunishment(PunishmentType.KICK, target, null,
                sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                sender.getName(), reason, null, id -> {
                    messages.send(sender, "moderation.kick", Map.of("target", target.getName()));
                    target.kickPlayer(messages.colorize(reason));
                });
        return true;
    }
}

