package me.moontimer.smpcore.util;

public final class DurationFormatter {
    private DurationFormatter() {
    }

    public static String formatSeconds(long seconds) {
        if (seconds < 0) {
            return "permanent";
        }
        long remaining = seconds;
        long weeks = remaining / 604800L;
        remaining %= 604800L;
        long days = remaining / 86400L;
        remaining %= 86400L;
        long hours = remaining / 3600L;
        remaining %= 3600L;
        long minutes = remaining / 60L;
        long secs = remaining % 60L;

        StringBuilder sb = new StringBuilder();
        appendUnit(sb, weeks, "w");
        appendUnit(sb, days, "d");
        appendUnit(sb, hours, "h");
        appendUnit(sb, minutes, "m");
        appendUnit(sb, secs, "s");

        if (sb.length() == 0) {
            return "0s";
        }
        return sb.toString();
    }

    private static void appendUnit(StringBuilder sb, long value, String unit) {
        if (value <= 0) {
            return;
        }
        sb.append(value).append(unit);
    }
}

