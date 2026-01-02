package me.moontimer.smpcore.listener;

import me.moontimer.smpcore.teleport.WarmupManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class WarmupListener implements Listener {
    private final WarmupManager warmupManager;

    public WarmupListener(WarmupManager warmupManager) {
        this.warmupManager = warmupManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        WarmupManager.PendingWarmup pending = warmupManager.getPending(player.getUniqueId());
        if (pending == null || !pending.isCancelOnMove()) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            warmupManager.cancel(player.getUniqueId());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        WarmupManager.PendingWarmup pending = warmupManager.getPending(player.getUniqueId());
        if (pending == null || !pending.isCancelOnDamage()) {
            return;
        }
        warmupManager.cancel(player.getUniqueId());
    }
}

