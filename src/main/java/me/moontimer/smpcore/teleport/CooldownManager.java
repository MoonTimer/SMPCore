package me.moontimer.smpcore.teleport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public boolean isOnCooldown(UUID uuid, String key) {
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) {
            return false;
        }
        Long until = playerCooldowns.get(key);
        if (until == null) {
            return false;
        }
        if (until <= now) {
            playerCooldowns.remove(key);
            return false;
        }
        return true;
    }

    public long getRemainingMillis(UUID uuid, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) {
            return 0L;
        }
        Long until = playerCooldowns.get(key);
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public void setCooldown(UUID uuid, String key, long durationSeconds) {
        if (durationSeconds <= 0) {
            return;
        }
        cooldowns.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>())
                .put(key, System.currentTimeMillis() + durationSeconds * 1000L);
    }

    public void clear(UUID uuid, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns != null) {
            playerCooldowns.remove(key);
        }
    }
}

