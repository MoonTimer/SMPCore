package me.moontimer.smpcore.command;

import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.BackManager;
import me.moontimer.smpcore.teleport.TeleportCause;
import me.moontimer.smpcore.teleport.TeleportManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BackCommand extends BaseCommand {
    private final BackManager backManager;
    private final TeleportManager teleportManager;

    public BackCommand(Plugin plugin, MessageService messages, BackManager backManager,
                       TeleportManager teleportManager) {
        super(plugin, messages);
        this.backManager = backManager;
        this.teleportManager = teleportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.back.use")) {
            return true;
        }
        Location location = backManager.getBackLocation(player.getUniqueId());
        if (location == null) {
            messages.send(player, "errors.no-back");
            return true;
        }
        int cooldown = plugin.getConfig().getInt("teleport.cooldowns.back", 0);
        TeleportManager.TeleportRequest request = new TeleportManager.TeleportRequest(
                "back",
                cooldown,
                -1,
                null,
                null,
                "smpcore.teleport.bypass.warmup",
                null,
                () -> messages.send(player, "teleport.back")
        );
        teleportManager.requestTeleport(player, location, TeleportCause.BACK, request);
        return true;
    }
}

