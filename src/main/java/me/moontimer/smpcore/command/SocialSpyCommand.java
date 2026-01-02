package me.moontimer.smpcore.command;

import me.moontimer.smpcore.chat.SocialSpyService;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SocialSpyCommand extends BaseCommand {
    private final SocialSpyService socialSpyService;

    public SocialSpyCommand(Plugin plugin, MessageService messages, SocialSpyService socialSpyService) {
        super(plugin, messages);
        this.socialSpyService = socialSpyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.staff.socialspy")) {
            return true;
        }
        boolean enabled = socialSpyService.toggle(player.getUniqueId());
        if (enabled) {
            messages.send(player, "socialspy.enabled");
        } else {
            messages.send(player, "socialspy.disabled");
        }
        return true;
    }
}

