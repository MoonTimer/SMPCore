package me.moontimer.smpcore.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.util.DurationFormatter;
import me.moontimer.smpcore.util.DurationParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class ReasonsCommand extends BaseCommand {
    private static final String PATH_BAN = "punishments.reason-presets.ban";
    private static final String PATH_MUTE = "punishments.reason-presets.mute";

    public ReasonsCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.mod.reasons")) {
            return true;
        }
        String filter = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        if (args.length > 1 || (!filter.isEmpty() && !filter.equals("ban") && !filter.equals("mute"))) {
            sendUsage(sender, "/reasons [ban|mute]");
            return true;
        }
        if (filter.isEmpty() || filter.equals("ban")) {
            sendGroup(sender, "Ban", PATH_BAN);
        }
        if (filter.isEmpty() || filter.equals("mute")) {
            sendGroup(sender, "Mute", PATH_MUTE);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("ban", "mute");
        }
        return List.of();
    }

    private void sendGroup(CommandSender sender, String label, String path) {
        List<ReasonPreset> presets = readPresets(path);
        messages.send(sender, "moderation.reasons-header", Map.of("type", label));
        if (presets.isEmpty()) {
            messages.send(sender, "moderation.reasons-empty");
            return;
        }
        String prefix = messages.get("prefix");
        for (ReasonPreset preset : presets) {
            String line = messages.format("moderation.reasons-line", Map.of(
                    "reason", preset.reason(),
                    "duration", formatDuration(preset.duration())
            ));
            sender.sendMessage(prefix + line);
        }
    }

    private List<ReasonPreset> readPresets(String path) {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> rawList = config.getMapList(path);
        List<ReasonPreset> presets = new ArrayList<>();
        for (Map<?, ?> entry : rawList) {
            Object reasonObj = entry.get("reason");
            Object durationObj = entry.get("duration");
            String reason = reasonObj == null ? "" : reasonObj.toString().trim();
            String duration = durationObj == null ? "" : durationObj.toString().trim();
            if (!reason.isEmpty()) {
                presets.add(new ReasonPreset(reason, duration));
            }
        }
        return presets;
    }

    private String formatDuration(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "permanent";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("permanent") || normalized.equals("perm") || normalized.equals("0") || normalized.equals("0s")) {
            return "permanent";
        }
        long seconds = DurationParser.parseSeconds(normalized);
        if (seconds > 0) {
            return DurationFormatter.formatSeconds(seconds);
        }
        return raw;
    }

    private record ReasonPreset(String reason, String duration) {
    }
}
