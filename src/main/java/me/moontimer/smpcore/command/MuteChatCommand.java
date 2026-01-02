package me.moontimer.smpcore.command;

import java.util.StringJoiner;
import me.moontimer.smpcore.chat.MuteChatService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.util.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class MuteChatCommand extends BaseCommand {
    private final MuteChatService muteChatService;

    public MuteChatCommand(Plugin plugin, MessageService messages, MuteChatService muteChatService) {
        super(plugin, messages);
        this.muteChatService = muteChatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.mod.mutechat")) {
            return true;
        }
        if (!plugin.getConfig().getBoolean("chat.mutechat-enabled", true)) {
            messages.send(sender, "errors.invalid-args", java.util.Map.of("usage", "/mutechat [duration] [reason]"));
            return true;
        }
        if (args.length == 0) {
            if (muteChatService.isMuted()) {
                muteChatService.unmute();
                Bukkit.broadcastMessage(messages.get("prefix") + messages.get("mutechat.disabled"));
            } else {
                muteChatService.mute(null, "");
                Bukkit.broadcastMessage(messages.get("prefix") + messages.get("mutechat.enabled"));
            }
            return true;
        }
        String first = args[0].toLowerCase();
        if (first.equals("off") || first.equals("disable")) {
            muteChatService.unmute();
            Bukkit.broadcastMessage(messages.get("prefix") + messages.get("mutechat.disabled"));
            return true;
        }
        long durationSeconds = DurationParser.parseSeconds(first);
        String reason = "";
        if (durationSeconds > 0) {
            if (args.length > 1) {
                StringJoiner joiner = new StringJoiner(" ");
                for (int i = 1; i < args.length; i++) {
                    joiner.add(args[i]);
                }
                reason = joiner.toString();
            }
        } else {
            durationSeconds = -1;
            StringJoiner joiner = new StringJoiner(" ");
            for (String arg : args) {
                joiner.add(arg);
            }
            reason = joiner.toString();
        }
        muteChatService.mute(durationSeconds > 0 ? durationSeconds : null, reason);
        Bukkit.broadcastMessage(messages.get("prefix") + messages.get("mutechat.enabled"));
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.List.of("off", "disable");
        }
        return java.util.List.of();
    }
}

