package me.moontimer.smpcore.command;

import java.util.Map;
import java.util.StringJoiner;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.moderation.PunishmentType;
import me.moontimer.smpcore.util.StaffNotify;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MuteCommand extends BaseCommand {
    private final PunishmentService punishments;

    public MuteCommand(Plugin plugin, MessageService messages, PunishmentService punishments) {
        super(plugin, messages);
        this.punishments = punishments;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.mod.mute")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/mute <player> [reason] [-s]");
            return true;
        }
        boolean silent = args[args.length - 1].equalsIgnoreCase("-s");
        String targetName = args[0];
        String reason = buildReason(args, 1, silent);
        if (reason.isEmpty()) {
            reason = plugin.getConfig().getString("punishments.default-reason", "Misconduct");
        }
        String finalReason = reason;

        Player online = Bukkit.getPlayerExact(targetName);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(targetName);
        String ip = online != null && online.getAddress() != null
                ? online.getAddress().getAddress().getHostAddress()
                : null;

        punishments.createPunishment(PunishmentType.MUTE, target, ip,
                sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                sender.getName(), reason, null, id -> {
                    String message = messages.format("moderation.mute", Map.of(
                            "target", target.getName() == null ? targetName : target.getName(),
                            "id", String.valueOf(id)
                    ));
                    String full = messages.get("prefix") + message;
                    if (silent) {
                        StaffNotify.broadcast(sender, full);
                    } else {
                        Bukkit.broadcastMessage(full);
                    }
                    if (online != null) {
                        String muteTemplate = messages.getRaw("punishments.mute-message");
                        String formatted = messages.colorize(muteTemplate
                                .replace("{reason}", finalReason)
                                .replace("{expires}", "permanent"));
                        online.sendMessage(formatted);
                    }
                });
        return true;
    }

    private String buildReason(String[] args, int start, boolean silent) {
        int end = silent ? args.length - 1 : args.length;
        if (start >= end) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = start; i < end; i++) {
            joiner.add(args[i]);
        }
        return joiner.toString();
    }
}

