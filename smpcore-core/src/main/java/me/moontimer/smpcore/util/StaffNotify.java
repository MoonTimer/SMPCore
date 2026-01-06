package me.moontimer.smpcore.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StaffNotify {
    private StaffNotify() {
    }

    public static void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("smpcore.staff.notify")) {
                player.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public static void broadcast(CommandSender exclude, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(exclude)) {
                continue;
            }
            if (player.hasPermission("smpcore.staff.notify")) {
                player.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }
}

