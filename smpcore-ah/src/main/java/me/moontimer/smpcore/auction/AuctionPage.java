package me.moontimer.smpcore.auction;

import java.util.List;

public record AuctionPage(List<AuctionListing> listings, int page, int totalPages, int totalCount) {
}
