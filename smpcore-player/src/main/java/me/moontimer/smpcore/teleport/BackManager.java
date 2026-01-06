package me.moontimer.smpcore.teleport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;

public class BackManager {
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();

    public void setBackLocation(UUID uuid, Location location) {
        if (location == null) {
            return;
        }
        lastLocations.put(uuid, location.clone());
    }

    public Location getBackLocation(UUID uuid) {
        Location location = lastLocations.get(uuid);
        return location == null ? null : location.clone();
    }

    public void clear(UUID uuid) {
        lastLocations.remove(uuid);
    }
}

