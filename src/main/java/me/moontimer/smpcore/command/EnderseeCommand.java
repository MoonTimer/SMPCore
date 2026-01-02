package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class EnderseeCommand extends BaseCommand {
    public EnderseeCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player viewer = requirePlayer(sender);
        if (viewer == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.staff.endersee")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/endersee <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        viewer.openInventory(target.getEnderChest());
        messages.send(viewer, "staff.endersee", Map.of("target", target.getName()));
        return true;
    }
}

