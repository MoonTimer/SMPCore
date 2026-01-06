package me.moontimer.smpcore.command;

import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.menu.StaffMenuService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MenuCommand extends BaseCommand {
    private final StaffMenuService menus;

    public MenuCommand(Plugin plugin, MessageService messages, StaffMenuService menus) {
        super(plugin, messages);
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.staff.menu")) {
            return true;
        }
        if (args.length == 0) {
            menus.openMainMenu(player);
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(player, "errors.player-not-found");
            return true;
        }
        menus.openPlayerMenu(player, target);
        return true;
    }
}
