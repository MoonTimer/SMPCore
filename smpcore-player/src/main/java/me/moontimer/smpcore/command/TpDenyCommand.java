package me.moontimer.smpcore.command;

import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.TpaService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TpDenyCommand extends BaseCommand {
    private final TpaService tpaService;

    public TpDenyCommand(Plugin plugin, MessageService messages, TpaService tpaService) {
        super(plugin, messages);
        this.tpaService = tpaService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.tpa.deny")) {
            return true;
        }

        TpaService.TpaRequest request;
        if (args.length > 0) {
            Player requester = Bukkit.getPlayerExact(args[0]);
            if (requester == null) {
                messages.send(player, "errors.player-not-found");
                return true;
            }
            request = tpaService.deny(player.getUniqueId(), requester.getUniqueId());
        } else {
            request = tpaService.getLatestRequest(player.getUniqueId());
            if (request != null) {
                request = tpaService.deny(player.getUniqueId(), request.from());
            }
        }

        if (request == null) {
            messages.send(player, "tpa.none");
            return true;
        }
        Player requester = Bukkit.getPlayer(request.from());
        if (requester != null) {
            messages.send(requester, "tpa.denied");
        }
        messages.send(player, "tpa.denied");
        return true;
    }
}

