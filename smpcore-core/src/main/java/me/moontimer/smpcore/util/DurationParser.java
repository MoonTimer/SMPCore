package me.moontimer.smpcore.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern TOKEN = Pattern.compile("(\\d+)([smhdw])");

    private DurationParser() {
    }

    public static long parseSeconds(String input) {
        if (input == null) {
            return -1;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return -1;
        }
        Matcher matcher = TOKEN.matcher(normalized);
        long total = 0L;
        int index = 0;
        while (matcher.find()) {
            if (matcher.start() != index) {
                return -1;
            }
            index = matcher.end();
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "s" -> total += value;
                case "m" -> total += value * 60L;
                case "h" -> total += value * 3600L;
                case "d" -> total += value * 86400L;
                case "w" -> total += value * 604800L;
                default -> {
                    return -1;
                }
            }
        }
        if (index != normalized.length()) {
            return -1;
        }
        return total > 0 ? total : -1;
    }
}

