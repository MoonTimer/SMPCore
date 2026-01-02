package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.chat.IgnoreService;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class IgnoreCommand extends BaseCommand {
    private final IgnoreService ignoreService;

    public IgnoreCommand(Plugin plugin, MessageService messages, IgnoreService ignoreService) {
        super(plugin, messages);
        this.ignoreService = ignoreService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.ignore.use")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/ignore <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(player, "errors.player-not-found");
            return true;
        }
        ignoreService.toggleIgnore(player.getUniqueId(), target.getUniqueId(), nowIgnoring -> {
            if (nowIgnoring) {
                messages.send(player, "ignore.added", Map.of("target", target.getName()));
            } else {
                messages.send(player, "ignore.removed", Map.of("target", target.getName()));
            }
        });
        return true;
    }
}

