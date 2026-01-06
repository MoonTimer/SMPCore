package me.moontimer.smpcore.command;

import java.util.List;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class HomesCommand extends BaseCommand {
    private final HomeService homeService;

    public HomesCommand(Plugin plugin, MessageService messages, HomeService homeService) {
        super(plugin, messages);
        this.homeService = homeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.home.list")) {
            return true;
        }
        homeService.listHomes(player.getUniqueId(), homes -> {
            String joined = String.join(", ", homes);
            messages.send(player, "teleport.homes-list", Map.of(
                    "count", String.valueOf(homes.size()),
                    "homes", joined
            ));
        });
        return true;
    }
}

