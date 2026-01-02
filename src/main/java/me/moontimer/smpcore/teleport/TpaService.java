package me.moontimer.smpcore.teleport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TpaService {
    private final Plugin plugin;
    private final MessageService messages;
    private final TeleportManager teleportManager;
    private final AuditService audit;
    private final Map<UUID, Map<UUID, TpaRequest>> requests = new ConcurrentHashMap<>();
    private final int timeoutSeconds = 60;

    public TpaService(Plugin plugin, MessageService messages, TeleportManager teleportManager, AuditService audit) {
        this.plugin = plugin;
        this.messages = messages;
        this.teleportManager = teleportManager;
        this.audit = audit;
    }

    public void sendRequest(Player from, Player to, TpaType type) {
        long expiresAt = System.currentTimeMillis() + timeoutSeconds * 1000L;
        TpaRequest request = new TpaRequest(from.getUniqueId(), to.getUniqueId(), type, expiresAt);
        requests.computeIfAbsent(to.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(from.getUniqueId(), request);

        messages.send(from, "tpa.sent", Map.of("target", to.getName()));
        messages.send(to, "tpa.received", Map.of("player", from.getName()));
        sendClickableResponse(to, from);

        Bukkit.getScheduler().runTaskLater(plugin, () -> expireRequest(request), timeoutSeconds * 20L);

        audit.log(from.getUniqueId(), "tpa_request", Map.of(
                "to", to.getUniqueId().toString(),
                "type", type.name()
        ));
    }

    public TpaRequest getLatestRequest(UUID target) {
        Map<UUID, TpaRequest> map = requests.get(target);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new ArrayList<>(map.values()).stream()
                .filter(req -> !req.isExpired())
                .max(Comparator.comparingLong(TpaRequest::expiresAt))
                .orElse(null);
    }

    public TpaRequest accept(UUID target, UUID sender) {
        TpaRequest request = removeRequest(target, sender);
        if (request == null || request.isExpired()) {
            return null;
        }
        return request;
    }

    public TpaRequest deny(UUID target, UUID sender) {
        return removeRequest(target, sender);
    }

    public int cancelOutgoing(UUID sender) {
        int removed = 0;
        for (Map<UUID, TpaRequest> map : requests.values()) {
            if (map.remove(sender) != null) {
                removed++;
            }
        }
        return removed;
    }

    private TpaRequest removeRequest(UUID target, UUID sender) {
        Map<UUID, TpaRequest> map = requests.get(target);
        if (map == null) {
            return null;
        }
        return map.remove(sender);
    }

    private void expireRequest(TpaRequest request) {
        Map<UUID, TpaRequest> map = requests.get(request.to());
        if (map == null) {
            return;
        }
        TpaRequest existing = map.get(request.from());
        if (existing == null || existing.isExpired()) {
            map.remove(request.from());
            Player from = Bukkit.getPlayer(request.from());
            Player to = Bukkit.getPlayer(request.to());
            if (from != null) {
                messages.send(from, "tpa.expired");
            }
            if (to != null) {
                messages.send(to, "tpa.expired");
            }
        }
    }

    private void sendClickableResponse(Player to, Player from) {
        String acceptRaw = messages.getRaw("tpa.click-accept");
        String denyRaw = messages.getRaw("tpa.click-deny");
        if ((acceptRaw == null || acceptRaw.isEmpty()) && (denyRaw == null || denyRaw.isEmpty())) {
            return;
        }
        Map<String, String> placeholders = Map.of("player", from.getName());
        String acceptText = TextUtil.applyPlaceholders(acceptRaw, placeholders);
        String denyText = TextUtil.applyPlaceholders(denyRaw, placeholders);
        String acceptHoverRaw = messages.getRaw("tpa.click-hover-accept");
        String denyHoverRaw = messages.getRaw("tpa.click-hover-deny");
        String acceptHoverText = TextUtil.applyPlaceholders(acceptHoverRaw, placeholders);
        String denyHoverText = TextUtil.applyPlaceholders(denyHoverRaw, placeholders);

        Component prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(messages.getRaw("prefix"));
        Component line = prefix;
        boolean added = false;

        if (acceptText != null && !acceptText.isEmpty()) {
            Component accept = LegacyComponentSerializer.legacyAmpersand().deserialize(acceptText)
                    .clickEvent(ClickEvent.runCommand("/tpaccept " + from.getName()));
            if (acceptHoverText != null && !acceptHoverText.isEmpty()) {
                accept = accept.hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(acceptHoverText)));
            }
            line = line.append(accept);
            added = true;
        }
        if (denyText != null && !denyText.isEmpty()) {
            if (added) {
                line = line.append(Component.text(" "));
            }
            Component deny = LegacyComponentSerializer.legacyAmpersand().deserialize(denyText)
                    .clickEvent(ClickEvent.runCommand("/tpdeny " + from.getName()));
            if (denyHoverText != null && !denyHoverText.isEmpty()) {
                deny = deny.hoverEvent(HoverEvent.showText(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(denyHoverText)));
            }
            line = line.append(deny);
        }
        to.sendMessage(line);
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public enum TpaType {
        TPA,
        TPAHERE
    }

    public record TpaRequest(UUID from, UUID to, TpaType type, long expiresAt) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}

