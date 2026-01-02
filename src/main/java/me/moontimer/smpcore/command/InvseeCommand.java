package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class InvseeCommand extends BaseCommand {
    public InvseeCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player viewer = requirePlayer(sender);
        if (viewer == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.staff.invsee")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/invsee <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        viewer.openInventory(target.getInventory());
        messages.send(viewer, "staff.invsee", Map.of("target", target.getName()));
        return true;
    }
}

