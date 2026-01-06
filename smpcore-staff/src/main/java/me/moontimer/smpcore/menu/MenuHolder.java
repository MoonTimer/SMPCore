package me.moontimer.smpcore.menu;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MenuHolder implements InventoryHolder {
    private final MenuType type;
    private final UUID target;
    private final int page;
    private Inventory inventory;

    public MenuHolder(MenuType type) {
        this(type, null, 0);
    }

    public MenuHolder(MenuType type, UUID target) {
        this(type, target, 0);
    }

    public MenuHolder(MenuType type, UUID target, int page) {
        this.type = type;
        this.target = target;
        this.page = page;
    }

    public MenuType getType() {
        return type;
    }

    public UUID getTarget() {
        return target;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
