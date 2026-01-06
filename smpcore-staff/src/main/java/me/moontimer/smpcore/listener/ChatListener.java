package me.moontimer.smpcore.listener;

import me.moontimer.smpcore.chat.MuteChatService;
import me.moontimer.smpcore.core.RankPrefixService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.moderation.PunishmentRecord;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.util.DurationFormatter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final PunishmentService punishmentService;
    private final MuteChatService muteChatService;
    private final MessageService messages;
    private final RankPrefixService rankPrefixService;

    public ChatListener(PunishmentService punishmentService, MuteChatService muteChatService,
                        MessageService messages, RankPrefixService rankPrefixService) {
        this.punishmentService = punishmentService;
        this.muteChatService = muteChatService;
        this.messages = messages;
        this.rankPrefixService = rankPrefixService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (muteChatService.isMuted() && !event.getPlayer().hasPermission("smpcore.mod.mutechat")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.get("prefix") + messages.get("errors.global-muted"));
            return;
        }

        PunishmentRecord mute = punishmentService.getActiveMute(event.getPlayer().getUniqueId());
        if (mute != null) {
            event.setCancelled(true);
            String expires = mute.expiresAt() == null
                    ? "permanent"
                    : DurationFormatter.formatSeconds(Math.max(0,
                    (mute.expiresAt() - System.currentTimeMillis()) / 1000L));
            String template = messages.getRaw("punishments.mute-message");
            String formatted = messages.colorize(template
                    .replace("{reason}", mute.reason())
                    .replace("{expires}", expires));
            event.getPlayer().sendMessage(formatted);
            return;
        }

        if (event.getPlayer().hasPermission("smpcore.chat.color")) {
            event.setMessage(messages.colorize(event.getMessage()));
        }

        String format = rankPrefixService.buildChatFormat(event.getPlayer());
        if (format != null && !format.isBlank()) {
            event.setFormat(format);
        }
    }
}

