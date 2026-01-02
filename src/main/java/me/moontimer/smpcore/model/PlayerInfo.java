package me.moontimer.smpcore.model;

public record PlayerInfo(
        String uuid,
        String name,
        String ip,
        long firstJoin,
        long lastJoin,
        long playtimeSeconds
) {
}

