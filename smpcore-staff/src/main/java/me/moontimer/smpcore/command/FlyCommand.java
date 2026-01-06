package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FlyCommand extends BaseCommand {
    public FlyCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!checkPermission(sender, "smpcore.staff.fly")) {
                return true;
            }
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
            if (!checkPermission(sender, "smpcore.fly.self")) {
                return true;
            }
        }

        boolean enable = !target.getAllowFlight();
        target.setAllowFlight(enable);
        if (!enable) {
            target.setFlying(false);
        }
        String key = enable ? "staff.fly-enabled" : "staff.fly-disabled";
        messages.send(sender, key, Map.of("target", target.getName()));
        if (!target.equals(sender)) {
            messages.send(target, key, Map.of("target", target.getName()));
        }
        return true;
    }
}

