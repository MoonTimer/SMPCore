package me.moontimer.smpcore.command;

import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class GamemodeCommand extends BaseCommand {
    public GamemodeCommand(Plugin plugin, MessageService messages) {
        super(plugin, messages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.staff.gamemode")) {
            return true;
        }
        String modeArg = args.length > 0 ? args[0] : resolveAlias(label);
        if (modeArg == null) {
            sendUsage(sender, "/gamemode <0|1|2|3> [player]");
            return true;
        }
        GameMode mode = parseMode(modeArg);
        if (mode == null) {
            sendUsage(sender, "/gamemode <0|1|2|3> [player]");
            return true;
        }
        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                messages.send(sender, "errors.player-not-found");
                return true;
            }
        } else {
            target = requirePlayer(sender);
            if (target == null) {
                return true;
            }
        }
        target.setGameMode(mode);
        messages.send(sender, "staff.gamemode", Map.of(
                "mode", mode.name().toLowerCase(Locale.ROOT),
                "target", target.getName()
        ));
        if (!target.equals(sender)) {
            messages.send(target, "staff.gamemode", Map.of(
                    "mode", mode.name().toLowerCase(Locale.ROOT),
                    "target", target.getName()
            ));
        }
        return true;
    }

    private GameMode parseMode(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private String resolveAlias(String label) {
        return switch (label.toLowerCase(Locale.ROOT)) {
            case "gms" -> "0";
            case "gmc" -> "1";
            case "gma" -> "2";
            case "gmsp" -> "3";
            default -> null;
        };
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.List.of("0", "1", "2", "3", "survival", "creative", "adventure", "spectator");
        }
        return null;
    }
}

