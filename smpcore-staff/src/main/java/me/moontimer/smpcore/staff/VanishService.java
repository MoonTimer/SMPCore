package me.moontimer.smpcore.staff;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class VanishService {
    private final Plugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishService(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean toggle(Player player) {
        if (isVanished(player.getUniqueId())) {
            show(player);
            return false;
        }
        hide(player);
        return true;
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public void hide(Player player) {
        vanished.add(player.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) {
                continue;
            }
            if (canSee(viewer)) {
                continue;
            }
            viewer.hidePlayer(plugin, player);
        }
    }

    public void show(Player player) {
        vanished.remove(player.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(plugin, player);
        }
    }

    public void handleJoin(Player joiner) {
        if (canSee(joiner)) {
            return;
        }
        for (UUID uuid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null) {
                joiner.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    public void handleQuit(Player player) {
        vanished.remove(player.getUniqueId());
    }

    private boolean canSee(Player player) {
        return player.hasPermission("smpcore.staff.vanish.see");
    }
}

