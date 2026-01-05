package me.moontimer.smpcore.auction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

public class AuctionItemFactory {
    private final HeadDatabaseHook headDatabase;

    public AuctionItemFactory(Plugin plugin) {
        this.headDatabase = new HeadDatabaseHook(plugin);
    }

    public ItemStack createItem(String spec, Player viewer) {
        if (spec == null || spec.isBlank()) {
            return new ItemStack(Material.STONE);
        }
        String trimmed = spec.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("hdb:")) {
            String id = trimmed.substring("hdb:".length()).trim();
            ItemStack head = headDatabase.getHead(id);
            if (head != null) {
                return head;
            }
            return new ItemStack(Material.PLAYER_HEAD);
        }
        if (lower.startsWith("texture:")) {
            String data = trimmed.substring("texture:".length()).trim();
            ItemStack head = createTexturedHead(data);
            return head == null ? new ItemStack(Material.PLAYER_HEAD) : head;
        }
        if (lower.equals("player_head") || lower.equals("player-head")) {
            return createPlayerHead(viewer);
        }
        Material material = Material.matchMaterial(trimmed.toUpperCase(Locale.ROOT));
        if (material == null) {
            material = Material.STONE;
        }
        return new ItemStack(material);
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (player == null) {
            return item;
        }
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTexturedHead(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        try {
            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object profile = profileClass.getDeclaredConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);
            Object property = propertyClass.getDeclaredConstructor(String.class, String.class)
                    .newInstance("textures", base64);
            Object properties = profileClass.getMethod("getProperties").invoke(profile);
            Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
            putMethod.invoke(properties, "textures", property);
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception ex) {
            return null;
        }
        item.setItemMeta(meta);
        return item;
    }
}
