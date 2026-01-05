package me.moontimer.smpcore.listener;

import java.util.Locale;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

public class CommandSecurityListener implements Listener {
    private final MessageService messages;

    public CommandSecurityListener(MessageService messages) {
        this.messages = messages;
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (event.getPlayer().hasPermission("smpcore.tabcomplete.bypass")) {
            return;
        }
        event.getCommands().clear();
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission("smpcore.command.plugins")) {
            return;
        }
        String message = event.getMessage();
        if (message == null || message.isEmpty()) {
            return;
        }
        String trimmed = message.trim();
        if (!trimmed.startsWith("/")) {
            return;
        }
        String raw = trimmed.substring(1);
        if (raw.isEmpty()) {
            return;
        }
        int spaceIndex = raw.indexOf(' ');
        String label = (spaceIndex == -1 ? raw : raw.substring(0, spaceIndex))
                .toLowerCase(Locale.ROOT);
        if (!isPluginListCommand(label)) {
            return;
        }
        event.setCancelled(true);
        messages.send(event.getPlayer(), "errors.no-permission");
    }

    private boolean isPluginListCommand(String label) {
        return switch (label) {
            case "plugins", "pl", "bukkit:plugins", "bukkit:pl",
                 "version", "ver", "bukkit:version", "bukkit:ver",
                 "about", "bukkit:about" -> true;
            default -> false;
        };
    }
}
