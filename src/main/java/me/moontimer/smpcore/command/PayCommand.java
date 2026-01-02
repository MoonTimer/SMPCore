package me.moontimer.smpcore.command;

import java.text.DecimalFormat;
import java.util.Map;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PayCommand extends BaseCommand {
    private final EconomyService economy;
    private final DecimalFormat format = new DecimalFormat("0.00");

    public PayCommand(Plugin plugin, MessageService messages, EconomyService economy) {
        super(plugin, messages);
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.economy.pay")) {
            return true;
        }
        if (!economy.isEnabled()) {
            messages.send(sender, "errors.economy-disabled");
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "/pay <player> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            sendUsage(sender, "/pay <player> <amount>");
            return true;
        }
        double min = plugin.getConfig().getDouble("economy.minimum-pay", 1.0);
        if (amount < min) {
            messages.send(sender, "economy.pay-minimum", Map.of("amount", format.format(min)));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            messages.send(sender, "errors.invalid-args", Map.of("usage", "/pay <player> <amount>"));
            return true;
        }
        economy.transfer(player.getUniqueId(), target.getUniqueId(), amount, result -> {
            if (!result.success()) {
                messages.send(player, "economy.pay-insufficient");
                return;
            }
            String formatted = format.format(amount);
            messages.send(player, "economy.pay-sent", Map.of(
                    "target", target.getName(),
                    "amount", formatted
            ));
            messages.send(target, "economy.pay-received", Map.of(
                    "player", player.getName(),
                    "amount", formatted
            ));
        });
        return true;
    }
}

