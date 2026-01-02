package me.moontimer.smpcore.core;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import me.moontimer.smpcore.util.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageService {
    private final JavaPlugin plugin;
    private FileConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public String getRaw(String path) {
        return messages.getString(path, "");
    }

    public String get(String path) {
        return colorize(getRaw(path));
    }

    public String format(String path, Map<String, String> placeholders) {
        String message = get(path);
        return TextUtil.applyPlaceholders(message, placeholders);
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Collections.emptyMap());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String prefix = get("prefix");
        String message = format(path, placeholders);
        if (!message.isEmpty()) {
            sender.sendMessage(prefix + message);
        }
    }

    public String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}

