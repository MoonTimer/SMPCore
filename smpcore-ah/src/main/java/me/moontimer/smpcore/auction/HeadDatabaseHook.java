package me.moontimer.smpcore.auction;

import java.lang.reflect.Method;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class HeadDatabaseHook {
    private final Plugin plugin;
    private Object api;
    private Method getItemHead;

    public HeadDatabaseHook(Plugin plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        Plugin hdb = plugin.getServer().getPluginManager().getPlugin("HeadDatabase");
        if (hdb == null || !hdb.isEnabled()) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
            api = apiClass.getDeclaredConstructor().newInstance();
            getItemHead = apiClass.getMethod("getItemHead", String.class);
        } catch (Exception ex) {
            api = null;
            getItemHead = null;
        }
    }

    public ItemStack getHead(String id) {
        if (api == null || getItemHead == null) {
            return null;
        }
        try {
            Object result = getItemHead.invoke(api, id);
            return result instanceof ItemStack item ? item : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
