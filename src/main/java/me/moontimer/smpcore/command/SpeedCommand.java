package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpeedCommand extends BaseCommand {
    public SpeedCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.staff.speed")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/speed <0-10> [player]");
            return true;
        }
        int value;
        try {
            value = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            sendUsage(sender, "/speed <0-10> [player]");
            return true;
        }
        value = Math.max(0, Math.min(10, value));
        float speed = value / 10.0f;

        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayerExact(args[1]);
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

        target.setWalkSpeed(speed);
        target.setFlySpeed(Math.max(0.1f, speed));
        messages.send(sender, "staff.speed", Map.of(
                "target", target.getName(),
                "speed", String.valueOf(value)
        ));
        if (!target.equals(sender)) {
            messages.send(target, "staff.speed", Map.of(
                    "target", target.getName(),
                    "speed", String.valueOf(value)
            ));
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        }
        return null;
    }
}

