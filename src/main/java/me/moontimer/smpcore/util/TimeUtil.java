package me.moontimer.smpcore.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private TimeUtil() {
    }

    public static String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0) {
            return "-";
        }
        return FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }
}

