package me.moontimer.smpcore.command;

import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.HomeService;
import me.moontimer.smpcore.teleport.TeleportCause;
import me.moontimer.smpcore.teleport.TeleportManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class HomeCommand extends BaseCommand {
    private final HomeService homeService;
    private final TeleportManager teleportManager;

    public HomeCommand(Plugin plugin, MessageService messages, HomeService homeService,
                       TeleportManager teleportManager) {
        super(plugin, messages);
        this.homeService = homeService;
        this.teleportManager = teleportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.home.use")) {
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "home";
        homeService.getHome(player.getUniqueId(), name, location -> {
            if (location == null) {
                messages.send(player, "errors.home-not-found");
                return;
            }
            int cooldown = plugin.getConfig().getInt("teleport.cooldowns.home", 0);
            TeleportManager.TeleportRequest request = new TeleportManager.TeleportRequest(
                    "home",
                    cooldown,
                    -1,
                    null,
                    null,
                    "smpcore.teleport.bypass.warmup",
                    null,
                    () -> messages.send(player, "teleport.home-teleport", Map.of("home", name))
            );
            teleportManager.requestTeleport(player, location, TeleportCause.HOME, request);
        });
        return true;
    }
}

