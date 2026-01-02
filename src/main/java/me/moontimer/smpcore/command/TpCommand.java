package me.moontimer.smpcore.command;

import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TpCommand extends BaseCommand {
    public TpCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/tp <player> | /tp <x> <y> <z> [world]");
            return true;
        }
        if (args.length >= 3) {
            Double x = parseCoordinate(args[0]);
            Double y = parseCoordinate(args[1]);
            Double z = parseCoordinate(args[2]);
            if (x != null && y != null && z != null) {
                if (!checkPermission(sender, "smpcore.staff.tppos")) {
                    return true;
                }
                if (args.length > 4) {
                    sendUsage(sender, "/tp <player> | /tp <x> <y> <z> [world]");
                    return true;
                }
                World world = player.getWorld();
                if (args.length == 4) {
                    world = Bukkit.getWorld(args[3]);
                    if (world == null) {
                        messages.send(sender, "errors.invalid-args",
                                Map.of("usage", "/tp <player> | /tp <x> <y> <z> [world]"));
                        return true;
                    }
                }
                Location location = new Location(world, x, y, z,
                        player.getLocation().getYaw(), player.getLocation().getPitch());
                player.teleport(location);
                messages.send(sender, "staff.tppos", Map.of(
                        "x", String.format(Locale.ROOT, "%.2f", x),
                        "y", String.format(Locale.ROOT, "%.2f", y),
                        "z", String.format(Locale.ROOT, "%.2f", z),
                        "world", world.getName()
                ));
                return true;
            }
        }
        if (args.length != 1) {
            sendUsage(sender, "/tp <player> | /tp <x> <y> <z> [world]");
            return true;
        }
        if (!checkPermission(sender, "smpcore.staff.tp")) {
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        player.teleport(target.getLocation());
        return true;
    }

    private Double parseCoordinate(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

