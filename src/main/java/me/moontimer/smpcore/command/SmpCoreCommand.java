package me.moontimer.smpcore.command;

import me.moontimer.smpcore.SmpCorePlugin;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class SmpCoreCommand extends BaseCommand {
    private final SmpCorePlugin core;

    public SmpCoreCommand(SmpCorePlugin plugin, MessageService messages) {
        super(plugin, messages);
        this.core = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender, "/smpcore <reload|migrate>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                if (!checkPermission(sender, "smpcore.config.reload")) {
                    return true;
                }
                core.reloadConfig();
                core.getMessages().reload();
                if (core.getTablistService() != null) {
                    core.getTablistService().reload();
                }
                messages.send(sender, "core.reloaded");
                return true;
            }
            case "migrate" -> {
                if (!checkPermission(sender, "smpcore.db.migrate")) {
                    return true;
                }
                core.getDatabase().migrate();
                messages.send(sender, "core.migrated");
                return true;
            }
            default -> {
                sendUsage(sender, "/smpcore <reload|migrate>");
                return true;
            }
        }
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.List.of("reload", "migrate");
        }
        return java.util.List.of();
    }
}

