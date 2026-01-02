package me.moontimer.smpcore.command;

import java.util.List;
import java.util.Map;
import me.moontimer.smpcore.combat.CombatService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.rtp.RtpService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RtpCommand extends BaseCommand {
    private final RtpService rtpService;
    private final CombatService combatService;

    public RtpCommand(Plugin plugin, MessageService messages, RtpService rtpService, CombatService combatService) {
        super(plugin, messages);
        this.rtpService = rtpService;
        this.combatService = combatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.rtp.use")) {
            return true;
        }
        if (combatService.isEnabled() && combatService.shouldBlockRtp()
                && !player.hasPermission("smpcore.combat.bypass")) {
            long remaining = combatService.getRemainingSeconds(player.getUniqueId());
            if (remaining > 0) {
                messages.send(player, "errors.in-combat", Map.of("time", remaining + "s"));
                return true;
            }
        }
        World world = player.getWorld();
        if (args.length > 0) {
            world = Bukkit.getWorld(args[0]);
            if (world == null) {
                messages.send(player, "errors.invalid-args", java.util.Map.of("usage", "/rtp [world]"));
                return true;
            }
            if (!player.hasPermission("smpcore.rtp.world." + world.getName())) {
                messages.send(player, "errors.no-permission");
                return true;
            }
        }
        List<String> blocked = plugin.getConfig().getStringList("rtp.blacklist-worlds");
        if (blocked.contains(world.getName())) {
            messages.send(player, "errors.rtp-disabled");
            return true;
        }
        rtpService.requestRtp(player, world);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return java.util.List.of();
    }
}

