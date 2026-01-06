package me.moontimer.smpcore.moderation;

public record PunishmentRecord(
        long id,
        PunishmentType type,
        String targetUuid,
        String targetName,
        String actorUuid,
        String actorName,
        String reason,
        long createdAt,
        Long expiresAt,
        Long revokedAt
) {
    public boolean isActive(long now) {
        if (revokedAt != null) {
            return false;
        }
        return expiresAt == null || expiresAt > now;
    }
}

