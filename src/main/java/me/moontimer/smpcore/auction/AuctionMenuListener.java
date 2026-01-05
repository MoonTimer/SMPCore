package me.moontimer.smpcore.auction;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AuctionMenuListener implements Listener {
    private final AuctionMenuService menus;

    public AuctionMenuListener(AuctionMenuService menus) {
        this.menus = menus;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof AuctionMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }
        menus.handleClick(player, holder, item);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof AuctionMenuHolder) {
            event.setCancelled(true);
        }
    }
}
