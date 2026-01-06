package me.moontimer.smpcore.listener;

import java.util.Map;
import me.moontimer.smpcore.combat.CombatService;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatListener implements Listener {
    private final CombatService combatService;
    private final MessageService messages;

    public CombatListener(CombatService combatService, MessageService messages) {
        this.combatService = combatService;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!combatService.isEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        if (!victim.hasPermission("smpcore.combat.bypass")) {
            combatService.tag(victim.getUniqueId());
        }
        if (!attacker.hasPermission("smpcore.combat.bypass")) {
            combatService.tag(attacker.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!combatService.isEnabled() || !combatService.shouldKillOnQuit()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("smpcore.combat.bypass")) {
            return;
        }
        if (combatService.isTagged(player.getUniqueId())) {
            player.setHealth(0.0);
            Bukkit.broadcastMessage(messages.get("prefix") + messages.format("combat.logout", Map.of(
                    "player", player.getName()
            )));
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            Object shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
