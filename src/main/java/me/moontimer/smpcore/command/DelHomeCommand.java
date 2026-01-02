package me.moontimer.smpcore.command;

import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DelHomeCommand extends BaseCommand {
    private final HomeService homeService;

    public DelHomeCommand(Plugin plugin, MessageService messages, HomeService homeService) {
        super(plugin, messages);
        this.homeService = homeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.home.delete")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/delhome <name>");
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        homeService.deleteHome(player.getUniqueId(), name, success -> {
            if (success) {
                messages.send(player, "teleport.home-deleted", Map.of("home", name));
            } else {
                messages.send(player, "errors.home-not-found");
            }
        });
        return true;
    }
}

