package me.moontimer.smpcore.command;

import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.PermissionUtil;
import me.moontimer.smpcore.teleport.HomeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SetHomeCommand extends BaseCommand {
    private final HomeService homeService;

    public SetHomeCommand(Plugin plugin, MessageService messages, HomeService homeService) {
        super(plugin, messages);
        this.homeService = homeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.home.sethome")) {
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "home";
        int defaultLimit = plugin.getConfig().getInt("homes.default-limit", 1);
        int limit = PermissionUtil.getMaxPermissionValue(player, "smpcore.home.limit.", defaultLimit);

        homeService.listHomes(player.getUniqueId(), homes -> {
            if (!homes.contains(name) && homes.size() >= limit) {
                messages.send(player, "errors.home-limit", Map.of("limit", String.valueOf(limit)));
                return;
            }
            homeService.setHome(player.getUniqueId(), name, player.getLocation(), success -> {
                if (success) {
                    messages.send(player, "teleport.home-set", Map.of("home", name));
                }
            });
        });
        return true;
    }
}

