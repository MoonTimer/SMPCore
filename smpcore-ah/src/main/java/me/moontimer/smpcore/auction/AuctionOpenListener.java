package me.moontimer.smpcore.auction;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.Nameable;

public class AuctionOpenListener implements Listener {
    private final Plugin plugin;
    private final AuctionMenuService menus;

    public AuctionOpenListener(Plugin plugin, AuctionMenuService menus) {
        this.plugin = plugin;
        this.menus = menus;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!plugin.getConfig().getBoolean("auctionhouse.enabled", true)) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("smpcore.auctionhouse.open")) {
            return;
        }
        if (matchesSign(block)) {
            event.setCancelled(true);
            menus.openBrowse(player, 0, false);
            return;
        }
        if (matchesNamedBlock(block)) {
            event.setCancelled(true);
            menus.openBrowse(player, 0, false);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.getConfig().getBoolean("auctionhouse.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("smpcore.auctionhouse.open")) {
            return;
        }
        if (matchesEntity(event.getRightClicked())) {
            event.setCancelled(true);
            menus.openBrowse(player, 0, false);
        }
    }

    private boolean matchesSign(Block block) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("auctionhouse.openers.sign.enabled", false)) {
            return false;
        }
        BlockState state = block.getState();
        if (!(state instanceof Sign sign)) {
            return false;
        }
        List<String> lines = config.getStringList("auctionhouse.openers.sign.lines");
        if (lines.isEmpty()) {
            return false;
        }
        String[] signLines = sign.getLines();
        for (int i = 0; i < Math.min(lines.size(), signLines.length); i++) {
            String expected = normalize(lines.get(i));
            if (expected.isEmpty()) {
                continue;
            }
            String actual = normalize(signLines[i]);
            if (!actual.equalsIgnoreCase(expected)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesNamedBlock(Block block) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("auctionhouse.openers.blocks.enabled", false)) {
            return false;
        }
        Set<Material> allowed = getBlockTypes(config.getStringList("auctionhouse.openers.blocks.types"));
        if (!allowed.isEmpty()) {
            Material type = block.getType();
            if (!allowed.contains(type)) {
                if (!(allowed.contains(Material.SHULKER_BOX) && type.name().endsWith("_SHULKER_BOX"))) {
                    return false;
                }
            }
        }
        BlockState state = block.getState();
        if (!(state instanceof Nameable nameable)) {
            return false;
        }
        String name = nameable.getCustomName();
        if (name == null || name.isEmpty()) {
            return false;
        }
        return matchesNameList(name, config.getStringList("auctionhouse.openers.blocks.names"));
    }

    private boolean matchesEntity(Entity entity) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("auctionhouse.openers.entities.enabled", false)) {
            return false;
        }
        String name = entity.getCustomName();
        if (name == null || name.isEmpty()) {
            return false;
        }
        return matchesNameList(name, config.getStringList("auctionhouse.openers.entities.names"));
    }

    private boolean matchesNameList(String name, List<String> names) {
        String normalized = normalize(name);
        for (String entry : names) {
            if (normalize(entry).equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return ChatColor.stripColor(value).trim();
    }

    private Set<Material> getBlockTypes(List<String> entries) {
        Set<Material> materials = new HashSet<>();
        for (String entry : entries) {
            Material material = Material.matchMaterial(entry.toUpperCase(Locale.ROOT));
            if (material != null) {
                materials.add(material);
            }
        }
        return materials;
    }
}
