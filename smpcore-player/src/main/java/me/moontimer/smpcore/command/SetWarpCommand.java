package me.moontimer.smpcore.command;

import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.WarpService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SetWarpCommand extends BaseCommand {
    private final WarpService warpService;

    public SetWarpCommand(Plugin plugin, MessageService messages, WarpService warpService) {
        super(plugin, messages);
        this.warpService = warpService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.warp.set")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/setwarp <name>");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        warpService.setWarp(name, player.getLocation(), player.getUniqueId(), success -> {
            if (success) {
                messages.send(player, "teleport.warp-set", Map.of("warp", name));
            }
        });
        return true;
    }
}

