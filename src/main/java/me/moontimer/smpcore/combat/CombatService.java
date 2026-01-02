package me.moontimer.smpcore.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.Plugin;

public class CombatService {
    private final Plugin plugin;
    private final Map<UUID, Long> lastCombat = new ConcurrentHashMap<>();

    public CombatService(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("combat.enabled", true);
    }

    public boolean shouldBlockRtp() {
        return plugin.getConfig().getBoolean("combat.block-rtp", true);
    }

    public boolean shouldBlockTpa() {
        return plugin.getConfig().getBoolean("combat.block-tpa", true);
    }

    public boolean shouldKillOnQuit() {
        return plugin.getConfig().getBoolean("combat.quit-kill", true);
    }

    public void tag(UUID uuid) {
        if (uuid == null) {
            return;
        }
        lastCombat.put(uuid, System.currentTimeMillis());
    }

    public boolean isTagged(UUID uuid) {
        return getRemainingSeconds(uuid) > 0;
    }

    public long getRemainingSeconds(UUID uuid) {
        if (uuid == null) {
            return 0L;
        }
        Long last = lastCombat.get(uuid);
        if (last == null) {
            return 0L;
        }
        long durationSeconds = plugin.getConfig().getLong("combat.tag-seconds", 10);
        long remainingMillis = last + durationSeconds * 1000L - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            lastCombat.remove(uuid);
            return 0L;
        }
        return (long) Math.ceil(remainingMillis / 1000.0);
    }
}
