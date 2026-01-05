package me.moontimer.smpcore.auction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AuctionMenuHolder implements InventoryHolder {
    private final AuctionMenuType type;
    private final int page;
    private final AuctionSort sort;
    private final String filter;
    private final boolean showBack;
    private Inventory inventory;

    public AuctionMenuHolder(AuctionMenuType type, int page, AuctionSort sort, String filter, boolean showBack) {
        this.type = type;
        this.page = page;
        this.sort = sort;
        this.filter = filter;
        this.showBack = showBack;
    }

    public AuctionMenuType getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public AuctionSort getSort() {
        return sort;
    }

    public String getFilter() {
        return filter;
    }

    public boolean isShowBack() {
        return showBack;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
