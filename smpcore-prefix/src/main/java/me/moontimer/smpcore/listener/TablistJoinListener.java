package me.moontimer.smpcore.listener;

import me.moontimer.smpcore.core.TablistService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TablistJoinListener implements Listener {
    private final TablistService tablistService;

    public TablistJoinListener(TablistService tablistService) {
        this.tablistService = tablistService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        tablistService.updatePlayer(event.getPlayer());
    }
}
