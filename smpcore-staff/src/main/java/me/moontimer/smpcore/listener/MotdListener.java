package me.moontimer.smpcore.listener;

import java.util.List;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.util.TextUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;

public class MotdListener implements Listener {
    private final Plugin plugin;
    private final MessageService messages;

    public MotdListener(Plugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (!plugin.getConfig().getBoolean("motd.enabled", true)) {
            return;
        }
        List<String> lines = plugin.getConfig().getStringList("motd.lines");
        if (lines == null || lines.isEmpty()) {
            return;
        }
        String joined = String.join("\n", lines);
        String withPlaceholders = TextUtil.applyPlaceholders(joined, Map.of(
                "online", String.valueOf(event.getNumPlayers()),
                "max", String.valueOf(event.getMaxPlayers())
        ));
        event.setMotd(messages.colorize(withPlaceholders));
    }
}

