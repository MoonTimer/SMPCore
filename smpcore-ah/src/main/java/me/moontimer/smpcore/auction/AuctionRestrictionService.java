package me.moontimer.smpcore.auction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class AuctionRestrictionService {
    private final Plugin plugin;
    private final List<AuctionRestrictionRule> rules = new ArrayList<>();

    public AuctionRestrictionService(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        rules.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("auctionhouse.restrictions");
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }
        List<Map<?, ?>> maps = section.getMapList("rules");
        for (Map<?, ?> map : maps) {
            Material type = parseMaterial(map.get("type"));
            String name = asString(map.get("name"));
            List<String> lore = asStringList(map.get("lore"));
            Set<Enchantment> enchants = parseEnchants(asStringList(map.get("enchants")));
            Integer damageMin = asInteger(map.get("damage-min"));
            Integer damageMax = asInteger(map.get("damage-max"));
            Boolean unbreakable = asBoolean(map.get("unbreakable"));
            Integer customModelData = asInteger(map.get("custom-model-data"));
            String reason = asString(map.get("reason"));
            rules.add(new AuctionRestrictionRule(type, name, lore, enchants, damageMin, damageMax,
                    unbreakable, customModelData, reason));
        }
    }

    public RestrictionResult check(ItemStack item) {
        if (rules.isEmpty()) {
            return RestrictionResult.allow();
        }
        for (AuctionRestrictionRule rule : rules) {
            if (rule.matches(item)) {
                String reason = rule.reason();
                return new RestrictionResult(false, reason == null || reason.isEmpty() ? null : reason);
            }
        }
        return RestrictionResult.allow();
    }

    private Material parseMaterial(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString().trim();
        if (raw.isEmpty()) {
            return null;
        }
        return Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
    }

    private Set<Enchantment> parseEnchants(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<Enchantment> result = new HashSet<>();
        for (String value : values) {
            Enchantment enchantment = parseEnchant(value);
            if (enchantment != null) {
                result.add(enchantment);
            }
        }
        return result;
    }

    private Enchantment parseEnchant(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String trimmed = value.toLowerCase(Locale.ROOT).trim();
        NamespacedKey key = NamespacedKey.fromString(trimmed);
        if (key == null) {
            key = NamespacedKey.minecraft(trimmed);
        }
        Enchantment enchantment = Enchantment.getByKey(key);
        if (enchantment != null) {
            return enchantment;
        }
        return Enchantment.getByName(trimmed.toUpperCase(Locale.ROOT));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    result.add(entry.toString());
                }
            }
            return result;
        }
        return List.of();
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    public record RestrictionResult(boolean allowed, String reason) {
        public static RestrictionResult allow() {
            return new RestrictionResult(true, null);
        }
    }
}
