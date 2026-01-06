package me.moontimer.smpcore.chat;

public class MuteChatService {
    private long mutedUntil = 0L;
    private String reason = "";

    public MuteChatService() {
    }

    public void mute(Long durationSeconds, String reason) {
        if (durationSeconds == null || durationSeconds <= 0) {
            mutedUntil = -1L;
        } else {
            mutedUntil = System.currentTimeMillis() + durationSeconds * 1000L;
        }
        this.reason = reason == null ? "" : reason;
    }

    public void unmute() {
        mutedUntil = 0L;
        reason = "";
    }

    public boolean isMuted() {
        if (mutedUntil == 0L) {
            return false;
        }
        if (mutedUntil < 0L) {
            return true;
        }
        if (System.currentTimeMillis() >= mutedUntil) {
            mutedUntil = 0L;
            return false;
        }
        return true;
    }

    public long getRemainingSeconds() {
        if (mutedUntil <= 0L) {
            return -1L;
        }
        long remaining = mutedUntil - System.currentTimeMillis();
        return Math.max(0L, remaining / 1000L);
    }

    public String getReason() {
        return reason;
    }
}
