package me.moontimer.smpcore.command;

import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.TeleportCause;
import me.moontimer.smpcore.teleport.TeleportManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpawnCommand extends BaseCommand {
    private final TeleportManager teleportManager;

    public SpawnCommand(Plugin plugin, MessageService messages, TeleportManager teleportManager) {
        super(plugin, messages);
        this.teleportManager = teleportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.spawn.use")) {
            return true;
        }
        Location spawn = player.getWorld().getSpawnLocation();
        int cooldown = plugin.getConfig().getInt("teleport.cooldowns.spawn", 0);
        TeleportManager.TeleportRequest request = new TeleportManager.TeleportRequest(
                "spawn",
                cooldown,
                -1,
                null,
                null,
                "smpcore.teleport.bypass.warmup",
                null,
                () -> messages.send(player, "teleport.spawn")
        );
        teleportManager.requestTeleport(player, spawn, TeleportCause.SPAWN, request);
        return true;
    }
}

