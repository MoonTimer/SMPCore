package me.moontimer.smpcore.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.TablistService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class TablistCommand extends BaseCommand {
    private final TablistService tablistService;

    public TablistCommand(Plugin plugin, MessageService messages, TablistService tablistService) {
        super(plugin, messages);
        this.tablistService = tablistService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.staff.tablist")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/tablist <setheader|setfooter|reload> <text>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "setheader", "header" -> {
                if (args.length < 2) {
                    sendUsage(sender, "/tablist setheader <text>");
                    return true;
                }
                List<String> lines = parseLines(args, 1);
                plugin.getConfig().set("tablist.header", lines);
                plugin.saveConfig();
                tablistService.updateAll();
                messages.send(sender, "tablist.header-updated");
                return true;
            }
            case "setfooter", "footer" -> {
                if (args.length < 2) {
                    sendUsage(sender, "/tablist setfooter <text>");
                    return true;
                }
                List<String> lines = parseLines(args, 1);
                plugin.getConfig().set("tablist.footer", lines);
                plugin.saveConfig();
                tablistService.updateAll();
                messages.send(sender, "tablist.footer-updated");
                return true;
            }
            case "reload" -> {
                plugin.reloadConfig();
                tablistService.reload();
                messages.send(sender, "tablist.reloaded");
                return true;
            }
            default -> {
                sendUsage(sender, "/tablist <setheader|setfooter|reload> <text>");
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
            return List.of("setheader", "setfooter", "reload");
        }
        return List.of();
    }
}

