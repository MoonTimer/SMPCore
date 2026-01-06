package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FeedCommand extends BaseCommand {
    public FeedCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.staff.feed")) {
            return true;
        }
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                messages.send(sender, "errors.player-not-found");
                return true;
            }
        } else {
            target = requirePlayer(sender);
            if (target == null) {
                return true;
            }
        }
        target.setFoodLevel(20);
        target.setSaturation(20f);
        messages.send(sender, "staff.fed", Map.of("target", target.getName()));
        if (!target.equals(sender)) {
            messages.send(target, "staff.fed", Map.of("target", target.getName()));
        }
        return true;
    }
}

