package me.moontimer.smpcore.command;

import me.moontimer.smpcore.core.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class HelpCommand extends BaseCommand {
    public HelpCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(messages.get("prefix") + messages.get("help.header"));
        sender.sendMessage(messages.get("prefix") + messages.get("help.player"));
        sender.sendMessage(messages.get("prefix") + messages.get("help.staff"));
        sender.sendMessage(messages.get("prefix") + messages.get("help.moderation"));
        return true;
    }
}

