package me.moontimer.smpcore.command;

import java.text.DecimalFormat;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.economy.EconomyService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BalanceCommand extends BaseCommand {
    private final EconomyService economy;
    private final DecimalFormat format = new DecimalFormat("0.00");

    public BalanceCommand(Plugin plugin, MessageService messages, EconomyService economy) {
        super(plugin, messages);
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.economy.balance")) {
            return true;
        }
        if (!economy.isEnabled()) {
            messages.send(sender, "errors.economy-disabled");
            return true;
        }
        economy.getBalance(player.getUniqueId(), balance -> {
            messages.send(player, "economy.balance", Map.of("balance", format.format(balance)));
        });
        return true;
    }
}

