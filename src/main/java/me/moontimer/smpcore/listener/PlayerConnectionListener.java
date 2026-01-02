package me.moontimer.smpcore.listener;

import me.moontimer.smpcore.chat.IgnoreService;
import me.moontimer.smpcore.core.PlayerService;
import me.moontimer.smpcore.core.TablistService;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.staff.VanishService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final PlayerService playerService;
    private final IgnoreService ignoreService;
    private final PunishmentService punishmentService;
    private final TablistService tablistService;
    private final VanishService vanishService;

    public PlayerConnectionListener(PlayerService playerService, IgnoreService ignoreService,
                                    PunishmentService punishmentService, TablistService tablistService,
                                    VanishService vanishService) {
        this.playerService = playerService;
        this.ignoreService = ignoreService;
        this.punishmentService = punishmentService;
        this.tablistService = tablistService;
        this.vanishService = vanishService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerService.handleJoin(event.getPlayer());
        ignoreService.loadIgnores(event.getPlayer().getUniqueId());
        punishmentService.refreshMute(event.getPlayer().getUniqueId());
        tablistService.updatePlayer(event.getPlayer());
        vanishService.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerService.handleQuit(event.getPlayer());
        ignoreService.unloadIgnores(event.getPlayer().getUniqueId());
        vanishService.handleQuit(event.getPlayer());
    }
}

