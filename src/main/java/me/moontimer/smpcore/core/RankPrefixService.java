package me.moontimer.smpcore.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import me.moontimer.smpcore.util.TextUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class RankPrefixService {
    private final Plugin plugin;
    private final MessageService messages;
    private volatile boolean enabled;
    private volatile boolean useLuckPerms;
    private volatile boolean useLuckPermsPrefix;
    private volatile boolean tabPrefixEnabled;
    private volatile String defaultGroup;
    private volatile String defaultPrefix;
    private volatile String chatFormat;
    private volatile String tabFormat;
    private volatile Map<String, String> groupPrefixes = Map.of();
    private volatile Map<String, String> groupLabels = Map.of();
    private volatile LuckPerms luckPerms;

    public RankPrefixService(Plugin plugin, MessageService messages) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.messages = Objects.requireNonNull(messages, "messages");
        reload();
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("rank-prefix.enabled", true);
        useLuckPerms = plugin.getConfig().getBoolean("rank-prefix.use-luckperms", true);
        useLuckPermsPrefix = plugin.getConfig().getBoolean("rank-prefix.use-luckperms-prefix", false);
        tabPrefixEnabled = plugin.getConfig().getBoolean("tablist.prefix-enabled", true);
        chatFormat = plugin.getConfig().getString("chat.format", "{prefix}{player}&7: {message}");
        tabFormat = plugin.getConfig().getString("tablist.name-format", "{prefix}{player}");
        defaultGroup = normalizeGroup(plugin.getConfig().getString("rank-prefix.default-group", "spieler"));
        defaultPrefix = plugin.getConfig().getString("rank-prefix.default-prefix", "&7Spieler &7| ");

        Map<String, String> prefixes = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("rank-prefix.groups");
        if (groups != null) {
            for (String key : groups.getKeys(false)) {
                ConfigurationSection groupSection = groups.getConfigurationSection(key);
                String prefix = null;
                String label = null;
                if (groupSection != null) {
                    prefix = groupSection.getString("prefix");
                    label = groupSection.getString("name");
                } else {
                    prefix = groups.getString(key);
                }
                String groupKey = normalizeGroup(key);
                if (prefix != null && !prefix.isBlank()) {
                    prefixes.put(groupKey, prefix);
                }
                if (label != null && !label.isBlank()) {
                    labels.put(groupKey, label);
                }
            }
        }
        groupPrefixes = Map.copyOf(prefixes);
        groupLabels = Map.copyOf(labels);

        luckPerms = resolveLuckPerms();
        if (useLuckPerms && luckPerms == null) {
            plugin.getLogger().warning("LuckPerms not found; using configured rank prefixes.");
        }
    }

    public boolean isTabPrefixEnabled() {
        return tabPrefixEnabled;
    }

    public String buildChatFormat(Player player) {
        if (player == null) {
            return null;
        }
        String format = chatFormat;
        if (format == null || format.isBlank()) {
            return null;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("prefix", escapeFormat(getPrefix(player)));
        placeholders.put("group", escapeFormat(getGroupLabel(player)));
        placeholders.put("player", "%1$s");
        placeholders.put("message", "%2$s");
        String line = TextUtil.applyPlaceholders(format, placeholders);
        return messages.colorize(line);
    }

    public String buildTabName(Player player) {
        if (player == null) {
            return null;
        }
        String format = tabFormat;
        if (format == null || format.isBlank()) {
            return null;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("prefix", getPrefix(player));
        placeholders.put("group", getGroupLabel(player));
        placeholders.put("player", player.getName());
        String line = TextUtil.applyPlaceholders(format, placeholders);
        return messages.colorize(line);
    }

    public String getPrefix(Player player) {
        if (!enabled || player == null) {
            return "";
        }
        User user = getUser(player);
        String prefix = null;
        if (useLuckPermsPrefix && user != null) {
            prefix = getMetaPrefix(user);
        }
        if (prefix == null || prefix.isBlank()) {
            String group = resolveGroup(user);
            prefix = groupPrefixes.get(group);
        }
        if (prefix == null || prefix.isBlank()) {
            prefix = defaultPrefix;
        }
        return prefix == null ? "" : prefix;
    }

    public String getGroupLabel(Player player) {
        if (!enabled || player == null) {
            return "";
        }
        User user = getUser(player);
        String group = resolveGroup(user);
        String label = groupLabels.get(group);
        if (label == null || label.isBlank()) {
            label = group;
        }
        return label == null ? "" : label;
    }

    private String resolveGroup(User user) {
        String group = user == null ? null : user.getPrimaryGroup();
        if (group == null || group.isBlank()) {
            group = defaultGroup;
        }
        return normalizeGroup(group);
    }

    private String normalizeGroup(String group) {
        if (group == null || group.isBlank()) {
            return "";
        }
        return group.toLowerCase(Locale.ROOT);
    }

    private User getUser(Player player) {
        LuckPerms lp = getLuckPerms();
        if (lp == null) {
            return null;
        }
        return lp.getUserManager().getUser(player.getUniqueId());
    }

    private String getMetaPrefix(User user) {
        LuckPerms lp = getLuckPerms();
        if (lp == null || user == null) {
            return null;
        }
        QueryOptions options = lp.getContextManager().getQueryOptions(user)
                .orElse(lp.getContextManager().getStaticQueryOptions());
        return user.getCachedData().getMetaData(options).getPrefix();
    }

    private LuckPerms getLuckPerms() {
        if (!useLuckPerms) {
            return null;
        }
        return luckPerms;
    }

    private LuckPerms resolveLuckPerms() {
        if (!useLuckPerms) {
            return null;
        }
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            return provider.getProvider();
        }
        return null;
    }

    private String escapeFormat(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replace("%", "%%");
    }
}
