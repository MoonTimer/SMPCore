package me.moontimer.smpcore.command;

import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.TeleportCause;
import me.moontimer.smpcore.teleport.TeleportManager;
import me.moontimer.smpcore.teleport.WarpService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WarpCommand extends BaseCommand {
    private final WarpService warpService;
    private final TeleportManager teleportManager;

    public WarpCommand(Plugin plugin, MessageService messages, WarpService warpService,
                       TeleportManager teleportManager) {
        super(plugin, messages);
        this.warpService = warpService;
        this.teleportManager = teleportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.warp.use")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/warp <name>");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        warpService.getWarp(name, location -> {
            if (location == null) {
                messages.send(player, "errors.warp-not-found");
                return;
            }
            int cooldown = plugin.getConfig().getInt("teleport.cooldowns.warp", 0);
            TeleportManager.TeleportRequest request = new TeleportManager.TeleportRequest(
                    "warp",
                    cooldown,
                    -1,
                    null,
                    null,
                    "smpcore.teleport.bypass.warmup",
                    null,
                    () -> messages.send(player, "teleport.warp-teleport", Map.of("warp", name))
            );
            teleportManager.requestTeleport(player, location, TeleportCause.WARP, request);
        });
        return true;
    }
}

