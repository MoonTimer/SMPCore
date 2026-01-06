package me.moontimer.smpcore.auction;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public record AuctionListing(
        long id,
        UUID sellerUuid,
        String sellerName,
        ItemStack item,
        double price,
        long createdAt,
        Long expiresAt,
        AuctionStatus status,
        UUID buyerUuid,
        String buyerName,
        Long soldAt,
        boolean serverListing,
        boolean unlimited,
        int stock,
        boolean soldCleared
) {
}
