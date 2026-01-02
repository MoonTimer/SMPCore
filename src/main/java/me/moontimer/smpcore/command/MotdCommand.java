package me.moontimer.smpcore.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class MotdCommand extends BaseCommand {
    public MotdCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.staff.motd")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/motd <set|reload> <text>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set" -> {
                if (args.length < 2) {
                    sendUsage(sender, "/motd set <text>");
                    return true;
                }
                List<String> lines = parseLines(args, 1);
                plugin.getConfig().set("motd.lines", lines);
                plugin.saveConfig();
                messages.send(sender, "motd.updated");
                return true;
            }
            case "reload" -> {
                plugin.reloadConfig();
                messages.send(sender, "motd.reloaded");
                return true;
            }
            default -> {
                sendUsage(sender, "/motd <set|reload> <text>");
                return true;
            }
        }
    }

    private List<String> parseLines(String[] args, int start) {
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = start; i < args.length; i++) {
            joiner.add(args[i]);
        }
        String raw = joiner.toString();
        String[] parts = raw.split("\\\\n");
        List<String> lines = new ArrayList<>();
        for (String part : parts) {
            lines.add(part);
        }
        return lines;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("set", "reload");
        }
        return List.of();
    }
}

