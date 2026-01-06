package me.moontimer.smpcore.listener;

import me.moontimer.smpcore.chat.IgnoreService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class IgnoreConnectionListener implements Listener {
    private final IgnoreService ignoreService;

    public IgnoreConnectionListener(IgnoreService ignoreService) {
        this.ignoreService = ignoreService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ignoreService.loadIgnores(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ignoreService.unloadIgnores(event.getPlayer().getUniqueId());
    }
}
