package me.moontimer.smpcore.auction;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class AuctionLogService {
    private final Plugin plugin;
    private final Gson gson = new Gson();

    public AuctionLogService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void log(String action, Map<String, Object> payload) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("auctionhouse.logging.enabled", false)) {
            return;
        }
        String pathValue = config.getString("auctionhouse.logging.file-path", "logs/auctionhouse.log");
        if (pathValue == null || pathValue.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        String json = payload == null ? "{}" : gson.toJson(payload);
        Path path = plugin.getDataFolder().toPath().resolve(pathValue);
        String line = now + "|" + action + "|" + json + System.lineSeparator();
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write auction log file: " + ex.getMessage());
        }
    }
}
