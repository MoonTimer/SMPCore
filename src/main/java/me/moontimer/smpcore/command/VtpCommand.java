package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.staff.VanishService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class VtpCommand extends BaseCommand {
    private final VanishService vanishService;

    public VtpCommand(Plugin plugin, MessageService messages, VanishService vanishService) {
        super(plugin, messages);
        this.vanishService = vanishService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.staff.vtp")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/vtp <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        if (!vanishService.isVanished(player.getUniqueId())) {
            vanishService.hide(player);
            messages.send(player, "staff.vanish-enabled", Map.of("target", player.getName()));
        }
        player.teleport(target.getLocation());
        messages.send(player, "staff.vtp", Map.of("target", target.getName()));
        return true;
    }
}
