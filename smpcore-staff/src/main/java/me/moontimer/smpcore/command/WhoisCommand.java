package me.moontimer.smpcore.command;

import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.PlayerService;
import me.moontimer.smpcore.model.PlayerInfo;
import me.moontimer.smpcore.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WhoisCommand extends BaseCommand {
    private final PlayerService playerService;

    public WhoisCommand(Plugin plugin, MessageService messages, PlayerService playerService) {
        super(plugin, messages);
        this.playerService = playerService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.staff.whois")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/whois <player>");
            return true;
        }
        Player online = Bukkit.getPlayerExact(args[0]);
        if (online != null) {
            playerService.getPlayerInfo(online.getUniqueId(), info -> {
                sendInfo(sender, info, online.getName(), online.getUniqueId().toString(),
                        online.getAddress() == null ? "" : online.getAddress().getAddress().getHostAddress());
            });
            return true;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
        if (offline.getUniqueId() == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        playerService.getPlayerInfo(offline.getUniqueId(), info -> {
            sendInfo(sender, info,
                    offline.getName() == null ? args[0] : offline.getName(),
                    offline.getUniqueId().toString(),
                    info == null ? "" : info.ip());
        });
        return true;
    }

    private void sendInfo(CommandSender sender, PlayerInfo info, String name, String uuid, String ip) {
        String first = info == null ? "-" : TimeUtil.formatTimestamp(info.firstJoin());
        String last = info == null ? "-" : TimeUtil.formatTimestamp(info.lastJoin());
        messages.send(sender, "staff.whois", Map.of(
                "name", name,
                "uuid", uuid,
                "ip", ip == null ? "" : ip,
                "first", first,
                "last", last
        ));
    }
}

