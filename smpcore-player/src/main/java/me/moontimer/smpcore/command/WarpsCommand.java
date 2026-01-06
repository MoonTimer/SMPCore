package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.WarpService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WarpsCommand extends BaseCommand {
    private final WarpService warpService;

    public WarpsCommand(Plugin plugin, MessageService messages, WarpService warpService) {
        super(plugin, messages);
        this.warpService = warpService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.warp.list")) {
            return true;
        }
        warpService.listWarps(warps -> {
            String joined = String.join(", ", warps);
            messages.send(player, "teleport.warps-list", Map.of(
                    "count", String.valueOf(warps.size()),
                    "warps", joined
            ));
        });
        return true;
    }
}

