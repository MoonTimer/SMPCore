package me.moontimer.smpcore.command;

import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TpHereCommand extends BaseCommand {
    public TpHereCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.staff.tphere")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/tphere <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        target.teleport(player.getLocation());
        return true;
    }
}

