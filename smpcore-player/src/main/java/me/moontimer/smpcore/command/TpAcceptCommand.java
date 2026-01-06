package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.combat.CombatService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.TeleportCause;
import me.moontimer.smpcore.teleport.TeleportManager;
import me.moontimer.smpcore.teleport.TpaService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TpAcceptCommand extends BaseCommand {
    private final TpaService tpaService;
    private final TeleportManager teleportManager;
    private final CombatService combatService;

    public TpAcceptCommand(Plugin plugin, MessageService messages, TpaService tpaService,
                           TeleportManager teleportManager, CombatService combatService) {
        super(plugin, messages);
        this.tpaService = tpaService;
        this.teleportManager = teleportManager;
        this.combatService = combatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.tpa.accept")) {
            return true;
        }

        TpaService.TpaRequest request;
        if (args.length > 0) {
            Player requester = Bukkit.getPlayerExact(args[0]);
            if (requester == null) {
                messages.send(player, "errors.player-not-found");
                return true;
            }
            request = tpaService.accept(player.getUniqueId(), requester.getUniqueId());
        } else {
            request = tpaService.getLatestRequest(player.getUniqueId());
            if (request != null) {
                request = tpaService.accept(player.getUniqueId(), request.from());
            }
        }

        if (request == null) {
            messages.send(player, "tpa.none");
            return true;
        }

        Player requester = Bukkit.getPlayer(request.from());
        if (requester == null) {
            messages.send(player, "errors.target-offline");
            return true;
        }

        Player teleportPlayer;
        Location destination;
        TeleportCause cause;
        if (request.type() == TpaService.TpaType.TPA) {
            teleportPlayer = requester;
            destination = player.getLocation();
            cause = TeleportCause.TPA;
        } else {
            teleportPlayer = player;
            destination = requester.getLocation();
            cause = TeleportCause.TPAHERE;
        }

        if (combatService.isEnabled() && combatService.shouldBlockTpa()
                && !teleportPlayer.hasPermission("smpcore.combat.bypass")) {
            long remaining = combatService.getRemainingSeconds(teleportPlayer.getUniqueId());
            if (remaining > 0) {
                messages.send(player, "errors.in-combat", Map.of("time", remaining + "s"));
                if (!teleportPlayer.equals(player)) {
                    messages.send(teleportPlayer, "errors.in-combat", Map.of("time", remaining + "s"));
                }
                return true;
            }
        }

        int cooldown = plugin.getConfig().getInt("teleport.cooldowns.tpa", 0);
        TeleportManager.TeleportRequest tpRequest = new TeleportManager.TeleportRequest(
                "tpa",
                cooldown,
                -1,
                null,
                null,
                "smpcore.teleport.bypass.warmup",
                "smpcore.tpa.cooldown.bypass",
                null
        );
        teleportManager.requestTeleport(teleportPlayer, destination, cause, tpRequest);
        messages.send(player, "tpa.accepted");
        messages.send(requester, "tpa.accepted");
        return true;
    }
}

