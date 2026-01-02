package me.moontimer.smpcore.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatService {
    private final MessageService messages;
    private final IgnoreService ignoreService;
    private final SocialSpyService socialSpyService;
    private final MuteChatService muteChatService;
    private final Map<UUID, UUID> lastReplies = new ConcurrentHashMap<>();

    public ChatService(MessageService messages, IgnoreService ignoreService,
                       SocialSpyService socialSpyService, MuteChatService muteChatService) {
        this.messages = messages;
        this.ignoreService = ignoreService;
        this.socialSpyService = socialSpyService;
        this.muteChatService = muteChatService;
    }

    public boolean sendPrivateMessage(CommandSender sender, Player target, String message) {
        if (sender instanceof Player player) {
            if (ignoreService.isIgnoring(target.getUniqueId(), player.getUniqueId())) {
                return false;
            }
            if (ignoreService.isIgnoring(player.getUniqueId(), target.getUniqueId())) {
                return false;
            }
        }

        String senderName = sender.getName();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("sender", senderName);
        placeholders.put("target", target.getName());
        placeholders.put("message", message);

        String sent = messages.format("msg.sent", placeholders);
        String received = messages.format("msg.received", placeholders);
        sender.sendMessage(messages.get("prefix") + sent);
        target.sendMessage(messages.get("prefix") + received);

        if (sender instanceof Player player) {
            lastReplies.put(player.getUniqueId(), target.getUniqueId());
            lastReplies.put(target.getUniqueId(), player.getUniqueId());
        } else {
            lastReplies.remove(target.getUniqueId());
        }

        broadcastSocialSpy(senderName, target.getName(), message, sender);
        return true;
    }

    public Player getReplyTarget(Player player) {
        UUID last = lastReplies.get(player.getUniqueId());
        if (last == null) {
            return null;
        }
        return Bukkit.getPlayer(last);
    }

    public MuteChatService getMuteChatService() {
        return muteChatService;
    }

    private void broadcastSocialSpy(String sender, String target, String message, CommandSender originalSender) {
        String spyMessage = messages.format("chat.socialspy-format", Map.of(
                "sender", sender,
                "target", target,
                "message", message
        ));
        for (UUID uuid : socialSpyService.getSpies()) {
            Player spy = Bukkit.getPlayer(uuid);
            if (spy == null || spy.equals(originalSender)) {
                continue;
            }
            if (!spy.hasPermission("smpcore.staff.socialspy")) {
                continue;
            }
            spy.sendMessage(messages.get("prefix") + messages.colorize(spyMessage));
        }
    }
}

