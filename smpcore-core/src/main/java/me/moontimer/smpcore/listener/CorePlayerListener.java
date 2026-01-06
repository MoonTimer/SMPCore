package me.moontimer.smpcore.listener;

import me.moontimer.smpcore.core.PlayerService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CorePlayerListener implements Listener {
    private final PlayerService playerService;

    public CorePlayerListener(PlayerService playerService) {
        this.playerService = playerService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerService.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerService.handleQuit(event.getPlayer());
    }
}
