package me.moontimer.smpcore.listener;

import me.moontimer.smpcore.moderation.PunishmentRecord;
import me.moontimer.smpcore.moderation.PunishmentService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PreLoginListener implements Listener {
    private final PunishmentService punishmentService;

    public PreLoginListener(PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress() == null ? "" : event.getAddress().getHostAddress();
        PunishmentRecord ban = punishmentService.findActiveBanSync(event.getUniqueId(), ip);
        if (ban == null) {
            return;
        }
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, punishmentService.getBanScreen(ban));
    }
}

