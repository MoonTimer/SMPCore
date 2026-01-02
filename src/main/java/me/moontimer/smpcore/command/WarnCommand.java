package me.moontimer.smpcore.command;

import java.util.Map;
import java.util.StringJoiner;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.moderation.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WarnCommand extends BaseCommand {
    private final PunishmentService punishments;

    public WarnCommand(Plugin plugin, MessageService messages, PunishmentService punishments) {
        super(plugin, messages);
        this.punishments = punishments;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.mod.warn")) {
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "/warn <player> <reason>");
            return true;
        }
        String targetName = args[0];
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = 1; i < args.length; i++) {
            joiner.add(args[i]);
        }
        String reason = joiner.toString();

        Player online = Bukkit.getPlayerExact(targetName);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(targetName);
        String ip = online != null && online.getAddress() != null
                ? online.getAddress().getAddress().getHostAddress()
                : null;

        punishments.createPunishment(PunishmentType.WARN, target, ip,
                sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                sender.getName(), reason, null, id -> {
                    messages.send(sender, "moderation.warn", Map.of(
                            "target", target.getName() == null ? targetName : target.getName(),
                            "id", String.valueOf(id)
                    ));
                    if (online != null) {
                        messages.send(online, "moderation.warn", Map.of(
                                "target", target.getName() == null ? targetName : target.getName(),
                                "id", String.valueOf(id)
                        ));
                    }
                });
        return true;
    }
}

