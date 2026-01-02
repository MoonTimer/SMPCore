package me.moontimer.smpcore.listener;

import me.moontimer.smpcore.teleport.BackManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

public class BackListener implements Listener {
    private final BackManager backManager;
    private final Plugin plugin;

    public BackListener(BackManager backManager, Plugin plugin) {
        this.backManager = backManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("back.on-death", true)) {
            return;
        }
        Player player = event.getEntity();
        backManager.setBackLocation(player.getUniqueId(), player.getLocation());
    }
}

