package me.moontimer.smpcore.moderation;

public record PunishmentSummary(
        int warnings,
        PunishmentRecord lastBan,
        PunishmentRecord lastMute,
        PunishmentRecord lastWarn
) {
}
