package me.moontimer.smpcore.command;

import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SetSpawnCommand extends BaseCommand {
    public SetSpawnCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.spawn.set")) {
            return true;
        }
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            return true;
        }
        location.getWorld().setSpawnLocation(location);
        messages.send(player, "teleport.spawn-set");
        return true;
    }
}
