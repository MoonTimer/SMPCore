package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TpPosCommand extends BaseCommand {
    public TpPosCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.staff.tppos")) {
            return true;
        }
        if (args.length < 3) {
            sendUsage(sender, "/tppos <x> <y> <z> [world]");
            return true;
        }
        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(args[0]);
            y = Double.parseDouble(args[1]);
            z = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            sendUsage(sender, "/tppos <x> <y> <z> [world]");
            return true;
        }
        World world = player.getWorld();
        if (args.length > 3) {
            world = Bukkit.getWorld(args[3]);
            if (world == null) {
                messages.send(sender, "errors.invalid-args", Map.of("usage", "/tppos <x> <y> <z> [world]"));
                return true;
            }
        }
        Location location = new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());
        player.teleport(location);
        messages.send(sender, "staff.tppos", Map.of(
                "x", String.format(java.util.Locale.ROOT, "%.2f", x),
                "y", String.format(java.util.Locale.ROOT, "%.2f", y),
                "z", String.format(java.util.Locale.ROOT, "%.2f", z),
                "world", world.getName()
        ));
        return true;
    }
}

