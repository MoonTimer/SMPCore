package me.moontimer.smpcore.command;

import java.util.Map;
import java.util.StringJoiner;
import me.moontimer.smpcore.chat.ChatService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.CooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MsgCommand extends BaseCommand {
    private final ChatService chatService;
    private final CooldownManager cooldowns;

    public MsgCommand(Plugin plugin, MessageService messages, ChatService chatService, CooldownManager cooldowns) {
        super(plugin, messages);
        this.chatService = chatService;
        this.cooldowns = cooldowns;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "smpcore.msg.use")) {
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "/msg <player> <message>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        if (sender instanceof Player player) {
            long cooldown = plugin.getConfig().getInt("chat.msg-cooldown-seconds", 0);
            if (cooldown > 0 && cooldowns.isOnCooldown(player.getUniqueId(), "msg")) {
                long remaining = cooldowns.getRemainingMillis(player.getUniqueId(), "msg") / 1000L;
                messages.send(player, "errors.on-cooldown", Map.of("time", String.valueOf(remaining) + "s"));
                return true;
            }
        }
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = 1; i < args.length; i++) {
            joiner.add(args[i]);
        }
        boolean sent = chatService.sendPrivateMessage(sender, target, joiner.toString());
        if (!sent) {
            messages.send(sender, "errors.cannot-message");
            return true;
        }
        if (sender instanceof Player player) {
            long cooldown = plugin.getConfig().getInt("chat.msg-cooldown-seconds", 0);
            if (cooldown > 0) {
                cooldowns.setCooldown(player.getUniqueId(), "msg", cooldown);
            }
        }
        return true;
    }
}

