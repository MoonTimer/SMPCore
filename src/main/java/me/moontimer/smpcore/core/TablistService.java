package me.moontimer.smpcore.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.moontimer.smpcore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class TablistService {
    private final Plugin plugin;
    private final MessageService messages;
    private BukkitTask task;

    public TablistService(Plugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) {
            return;
        }
        int interval = Math.max(2, plugin.getConfig().getInt("tablist.update-interval-seconds", 10));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, interval * 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reload() {
        start();
        updateAll();
    }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setPlayerListHeaderFooter("", "");
            }
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void updatePlayer(Player player) {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) {
            player.setPlayerListHeaderFooter("", "");
            return;
        }
        List<String> headerLines = plugin.getConfig().getStringList("tablist.header");
        List<String> footerLines = plugin.getConfig().getStringList("tablist.footer");
        Map<String, String> placeholders = buildPlaceholders(player);
        String header = formatLines(headerLines, placeholders);
        String footer = formatLines(footerLines, placeholders);
        player.setPlayerListHeaderFooter(header, footer);
    }

    private Map<String, String> buildPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        placeholders.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        placeholders.put("world", player.getWorld().getName());
        placeholders.put("ping", String.valueOf(player.getPing()));
        return placeholders;
    }

    private String formatLines(List<String> lines, Map<String, String> placeholders) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        String joined = String.join("\n", lines);
        String withPlaceholders = TextUtil.applyPlaceholders(joined, placeholders);
        return messages.colorize(withPlaceholders);
    }
}

