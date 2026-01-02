package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.combat.CombatService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.CooldownManager;
import me.moontimer.smpcore.teleport.TpaService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TpaCommand extends BaseCommand {
    private final TpaService tpaService;
    private final CooldownManager cooldowns;
    private final CombatService combatService;

    public TpaCommand(Plugin plugin, MessageService messages, TpaService tpaService, CooldownManager cooldowns,
                      CombatService combatService) {
        super(plugin, messages);
        this.tpaService = tpaService;
        this.cooldowns = cooldowns;
        this.combatService = combatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.tpa.send")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/tpa <player>");
            return true;
        }
        if (combatService.isEnabled() && combatService.shouldBlockTpa()
                && !player.hasPermission("smpcore.combat.bypass")) {
            long remaining = combatService.getRemainingSeconds(player.getUniqueId());
            if (remaining > 0) {
                messages.send(player, "errors.in-combat", Map.of("time", remaining + "s"));
                return true;
            }
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            messages.send(player, "errors.player-not-found");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            messages.send(player, "errors.invalid-args", Map.of("usage", "/tpa <player>"));
            return true;
        }
        if (!player.hasPermission("smpcore.tpa.cooldown.bypass")) {
            if (cooldowns.isOnCooldown(player.getUniqueId(), "tpa")) {
                long remaining = cooldowns.getRemainingMillis(player.getUniqueId(), "tpa") / 1000L;
                messages.send(player, "errors.on-cooldown", Map.of("time", String.valueOf(remaining) + "s"));
                return true;
            }
        }
        tpaService.sendRequest(player, target, TpaService.TpaType.TPA);
        int cooldown = plugin.getConfig().getInt("teleport.cooldowns.tpa", 0);
        if (cooldown > 0 && !player.hasPermission("smpcore.tpa.cooldown.bypass")) {
            cooldowns.setCooldown(player.getUniqueId(), "tpa", cooldown);
        }
        return true;
    }
}

