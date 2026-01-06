package me.moontimer.smpcore.chat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SocialSpyService {
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();

    public boolean toggle(UUID uuid) {
        if (spies.contains(uuid)) {
            spies.remove(uuid);
            return false;
        }
        spies.add(uuid);
        return true;
    }

    public boolean isEnabled(UUID uuid) {
        return spies.contains(uuid);
    }

    public Set<UUID> getSpies() {
        return Set.copyOf(spies);
    }
}
