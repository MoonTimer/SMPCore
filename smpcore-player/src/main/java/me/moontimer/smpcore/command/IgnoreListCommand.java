package me.moontimer.smpcore.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.moontimer.smpcore.chat.IgnoreService;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class IgnoreListCommand extends BaseCommand {
    private final IgnoreService ignoreService;

    public IgnoreListCommand(Plugin plugin, MessageService messages, IgnoreService ignoreService) {
        super(plugin, messages);
        this.ignoreService = ignoreService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.ignore.use")) {
            return true;
        }
        ignoreService.listIgnores(player.getUniqueId(), uuids -> {
            List<String> names = new ArrayList<>();
            for (UUID uuid : uuids) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                names.add(offline.getName() == null ? uuid.toString() : offline.getName());
            }
            String joined = String.join(", ", names);
            messages.send(player, "ignore.list", Map.of(
                    "count", String.valueOf(names.size()),
                    "players", joined
            ));
        });
        return true;
    }
}

