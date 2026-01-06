package me.moontimer.smpcore.auction;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class AuctionRestrictionRule {
    private final Material type;
    private final String namePattern;
    private final List<String> lorePatterns;
    private final Set<Enchantment> enchants;
    private final Integer damageMin;
    private final Integer damageMax;
    private final Boolean unbreakable;
    private final Integer customModelData;
    private final String reason;

    public AuctionRestrictionRule(Material type, String namePattern, List<String> lorePatterns,
                                  Set<Enchantment> enchants, Integer damageMin, Integer damageMax,
                                  Boolean unbreakable, Integer customModelData, String reason) {
        this.type = type;
        this.namePattern = namePattern;
        this.lorePatterns = lorePatterns == null ? List.of() : List.copyOf(lorePatterns);
        this.enchants = enchants == null ? Collections.emptySet() : Set.copyOf(enchants);
        this.damageMin = damageMin;
        this.damageMax = damageMax;
        this.unbreakable = unbreakable;
        this.customModelData = customModelData;
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (type != null && item.getType() != type) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (namePattern != null && !namePattern.isEmpty()) {
            String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();
            if (!matchesText(name, namePattern)) {
                return false;
            }
        }
        if (!lorePatterns.isEmpty()) {
            List<String> lore = meta != null && meta.hasLore() ? meta.getLore() : List.of();
            if (!matchesAnyPattern(lore, lorePatterns)) {
                return false;
            }
        }
        if (!enchants.isEmpty()) {
            boolean found = false;
            for (Enchantment enchantment : enchants) {
                if (item.getEnchantments().containsKey(enchantment)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        if (customModelData != null) {
            if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) {
                return false;
            }
        }
        if (unbreakable != null) {
            boolean itemUnbreakable = meta != null && meta.isUnbreakable();
            if (itemUnbreakable != unbreakable) {
                return false;
            }
        }
        if (damageMin != null || damageMax != null) {
            int damage = 0;
            if (meta instanceof Damageable damageable) {
                damage = damageable.getDamage();
            }
            if (damageMin != null && damage < damageMin) {
                return false;
            }
            if (damageMax != null && damage > damageMax) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAnyPattern(List<String> lines, List<String> patterns) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            for (String pattern : patterns) {
                if (matchesText(line, pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesText(String text, String pattern) {
        if (text == null) {
            return false;
        }
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        if (pattern.toLowerCase(Locale.ROOT).startsWith("regex:")) {
            String expr = pattern.substring("regex:".length());
            try {
                return Pattern.compile(expr, Pattern.CASE_INSENSITIVE).matcher(text).find();
            } catch (Exception ex) {
                return false;
            }
        }
        return text.toLowerCase(Locale.ROOT).contains(pattern.toLowerCase(Locale.ROOT));
    }
}
