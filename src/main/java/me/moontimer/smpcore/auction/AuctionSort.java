package me.moontimer.smpcore.auction;

import java.util.Locale;

public enum AuctionSort {
    TIME,
    PRICE;

    public static AuctionSort fromConfig(String value) {
        if (value == null || value.isEmpty()) {
            return TIME;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("price")) {
            return PRICE;
        }
        return TIME;
    }
}
