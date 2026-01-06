package me.moontimer.smpcore.core;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean has(CommandSender sender, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        return sender.hasPermission(permission);
    }

    public static int getMaxPermissionValue(Player player, String prefix, int fallback) {
        int max = fallback;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            if (!perm.startsWith(prefix)) {
                continue;
            }
            String value = perm.substring(prefix.length());
            try {
                int parsed = Integer.parseInt(value);
                if (parsed > max) {
                    max = parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }
}

