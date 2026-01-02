package me.moontimer.smpcore.command;

import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ClearChatCommand extends BaseCommand {
    public ClearChatCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.staff.clearchat")) {
            return true;
        }
        for (int i = 0; i < 120; i++) {
            Bukkit.broadcastMessage("");
        }
        Bukkit.broadcastMessage(messages.get("prefix") + messages.get("staff.clearchat"));
        return true;
    }
}

