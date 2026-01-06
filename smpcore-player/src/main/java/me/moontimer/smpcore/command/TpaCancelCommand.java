package me.moontimer.smpcore.command;

import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.TpaService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TpaCancelCommand extends BaseCommand {
    private final TpaService tpaService;

    public TpaCancelCommand(Plugin plugin, MessageService messages, TpaService tpaService) {
        super(plugin, messages);
        this.tpaService = tpaService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.tpa.cancel")) {
            return true;
        }
        int removed = tpaService.cancelOutgoing(player.getUniqueId());
        if (removed > 0) {
            messages.send(player, "tpa.cancelled");
        } else {
            messages.send(player, "tpa.none");
        }
        return true;
    }
}

