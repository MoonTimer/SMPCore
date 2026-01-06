package me.moontimer.smpcore.listener;

import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.staff.VanishService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StaffConnectionListener implements Listener {
    private final PunishmentService punishments;
    private final VanishService vanishService;

    public StaffConnectionListener(PunishmentService punishments, VanishService vanishService) {
        this.punishments = punishments;
        this.vanishService = vanishService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        punishments.refreshMute(event.getPlayer().getUniqueId());
        vanishService.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        vanishService.handleQuit(event.getPlayer());
    }
}
