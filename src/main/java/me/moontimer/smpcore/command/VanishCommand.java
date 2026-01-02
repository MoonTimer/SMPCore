package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.staff.VanishService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class VanishCommand extends BaseCommand {
    private final VanishService vanishService;

    public VanishCommand(Plugin plugin, MessageService messages, VanishService vanishService) {
        super(plugin, messages);
        this.vanishService = vanishService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!checkPermission(sender, "smpcore.staff.vanish.others")) {
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
            if (!checkPermission(sender, "smpcore.staff.vanish")) {
                return true;
            }
        }

        boolean enabled = vanishService.toggle(target);
        String key = enabled ? "staff.vanish-enabled" : "staff.vanish-disabled";
        messages.send(sender, key, Map.of("target", target.getName()));
        if (!target.equals(sender)) {
            messages.send(target, key, Map.of("target", target.getName()));
        }
        return true;
    }
}

