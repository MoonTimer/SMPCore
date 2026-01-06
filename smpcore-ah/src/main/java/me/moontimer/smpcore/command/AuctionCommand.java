package me.moontimer.smpcore.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.moontimer.smpcore.auction.AuctionMenuService;
import me.moontimer.smpcore.auction.AuctionService;
import me.moontimer.smpcore.auction.AuctionService.AuctionCreateResult;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class AuctionCommand extends BaseCommand {
    private final AuctionService auctionService;
    private final AuctionMenuService menus;

    public AuctionCommand(Plugin plugin, MessageService messages, AuctionService auctionService,
                          AuctionMenuService menus) {
        super(plugin, messages);
        this.auctionService = auctionService;
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("auctionhouse.enabled", true)) {
            messages.send(sender, "auctionhouse.errors.disabled");
            return true;
        }
        if (args.length == 0) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (!checkPermission(sender, "smpcore.auctionhouse.open")) {
                return true;
            }
            menus.openBrowse(player, 0, false);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "open" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!checkPermission(sender, "smpcore.auctionhouse.open")) {
                    return true;
                }
                menus.openBrowse(player, 0, false);
                return true;
            }
            case "menu" -> {
                if (!checkPermission(sender, "smpcore.auctionhouse.show")) {
                    return true;
                }
                String targetName = args.length >= 2 ? args[1] : null;
                Player target = resolveTarget(sender, targetName);
                if (target == null) {
                    if (targetName != null) {
                        messages.send(sender, "errors.player-not-found");
                    } else {
                        sendUsage(sender, "/ah menu <player>");
                    }
                    return true;
                }
                menus.openBrowse(target, 0, true);
                return true;
            }
            case "show" -> {
                if (!checkPermission(sender, "smpcore.auctionhouse.show")) {
                    return true;
                }
                String targetName = args.length >= 2 ? args[1] : null;
                Player target = resolveTarget(sender, targetName);
                if (target == null) {
                    if (targetName != null) {
                        messages.send(sender, "errors.player-not-found");
                    } else {
                        sendUsage(sender, "/ah show <player>");
                    }
                    return true;
                }
                menus.openBrowse(target, 0, false);
                return true;
            }
            case "reload" -> {
                if (!checkPermission(sender, "smpcore.auctionhouse.reload")) {
                    return true;
                }
                plugin.reloadConfig();
                messages.reload();
                auctionService.reload();
                messages.send(sender, "auctionhouse.reloaded");
                return true;
            }
            case "help" -> {
                messages.getList("auctionhouse.help").forEach(line -> sender.sendMessage(messages.get("prefix") + line));
                return true;
            }
            case "search" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!checkPermission(sender, "smpcore.auctionhouse.search")) {
                    return true;
                }
                if (args.length < 2) {
                    sendUsage(sender, "/ah search <filter>");
                    return true;
                }
                String filter = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                menus.openSearch(player, filter, 0, false);
                return true;
            }
            case "sell" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!checkPermission(sender, "smpcore.auctionhouse.sell")) {
                    return true;
                }
                if (args.length < 2) {
                    sendUsage(sender, "/ah sell <price>");
                    return true;
                }
                Double price = parsePrice(sender, args[1]);
                if (price == null) {
                    return true;
                }
                handleListing(player, price, false, false, 1);
                return true;
            }
            case "list" -> {
                return handleServerListing(sender, args, false);
            }
            case "ulist" -> {
                return handleServerListing(sender, args, true);
            }
            case "selling" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!checkPermission(sender, "smpcore.auctionhouse.selling")) {
                    return true;
                }
                menus.openSelling(player, 0, false);
                return true;
            }
            case "sold" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!checkPermission(sender, "smpcore.auctionhouse.sold")) {
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
                    auctionService.clearSold(player.getUniqueId(), count -> {
                        if (count > 0) {
                            messages.send(player, "auctionhouse.sold.cleared", Map.of("count", String.valueOf(count)));
                        } else {
                            messages.send(player, "auctionhouse.sold.empty");
                        }
                    });
                    return true;
                }
                menus.openSold(player, 0, false);
                return true;
            }
            case "expired" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!checkPermission(sender, "smpcore.auctionhouse.expired")) {
                    return true;
                }
                menus.openExpired(player, 0, false);
                return true;
            }
            case "cancel" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!checkPermission(sender, "smpcore.auctionhouse.cancel")) {
                    return true;
                }
                auctionService.cancelAll(player.getUniqueId(), count -> {
                    if (count > 0) {
                        messages.send(player, "auctionhouse.cancelled-all", Map.of("count", String.valueOf(count)));
                    } else {
                        messages.send(player, "auctionhouse.cancelled-none");
                    }
                });
                return true;
            }
            case "return" -> {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!checkPermission(sender, "smpcore.auctionhouse.return")) {
                    return true;
                }
                returnItems(player);
                return true;
            }
            default -> {
                sendUsage(sender, "/ah help");
                return true;
            }
        }
    }

    private boolean handleServerListing(CommandSender sender, String[] args, boolean unlimited) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.auctionhouse.server")) {
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, unlimited ? "/ah ulist <price> [count]" : "/ah list <price> [count]");
            return true;
        }
        Double price = parsePrice(sender, args[1]);
        if (price == null) {
            return true;
        }
        int count = 1;
        if (args.length >= 3) {
            try {
                count = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                messages.send(sender, "auctionhouse.errors.invalid-count");
                return true;
            }
        }
        if (count < 1) {
            messages.send(sender, "auctionhouse.errors.invalid-count");
            return true;
        }
        handleListing(player, price, true, unlimited, count);
        return true;
    }

    private Player resolveTarget(CommandSender sender, String name) {
        if (name == null || name.isEmpty()) {
            if (sender instanceof Player player) {
                return player;
            }
            return null;
        }
        return Bukkit.getPlayerExact(name);
    }

    private Double parsePrice(CommandSender sender, String raw) {
        double price;
        try {
            price = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            messages.send(sender, "auctionhouse.errors.invalid-price");
            return null;
        }
        if (price <= 0.0) {
            messages.send(sender, "auctionhouse.errors.invalid-price");
            return null;
        }
        return price;
    }

    private void handleListing(Player player, double price, boolean serverListing, boolean unlimited, int stock) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            messages.send(player, "auctionhouse.errors.no-item");
            return;
        }
        AuctionService.AuctionCreateResult restriction = validateListing(item, price);
        if (restriction != null) {
            String key = restriction.messageKey() == null ? "auctionhouse.errors.create-failed" : restriction.messageKey();
            if ("auctionhouse.errors.price-range".equals(key)) {
                messages.send(player, key, Map.of(
                        "min", formatPrice(auctionService.getMinPrice()),
                        "max", formatPrice(auctionService.getMaxPrice())
                ));
            } else if (restriction.reason() != null && !restriction.reason().isEmpty()) {
                messages.send(player, key, Map.of("reason", restriction.reason()));
            } else if ("auctionhouse.errors.restricted".equals(key)) {
                messages.send(player, key, Map.of("reason", "-"));
            } else {
                messages.send(player, key);
            }
            return;
        }
        double fee = serverListing ? 0.0 : auctionService.calculateListingFee(price);
        AuctionMenuService.PendingListing pending = new AuctionMenuService.PendingListing(
                item.clone(), price, serverListing, unlimited, stock, fee, !serverListing);
        menus.beginListing(player, pending);
    }

    private AuctionCreateResult validateListing(ItemStack item, double price) {
        if (price < auctionService.getMinPrice() || price > auctionService.getMaxPrice()) {
            return AuctionCreateResult.failure("auctionhouse.errors.price-range");
        }
        AuctionService.AuctionCreateResult restriction = null;
        var result = auctionService.getRestrictions().check(item);
        if (result != null && !result.allowed()) {
            restriction = AuctionCreateResult.failure(result.reason(), "auctionhouse.errors.restricted");
        }
        return restriction;
    }

    private void returnItems(Player player) {
        boolean drop = plugin.getConfig().getBoolean("auctionhouse.return.drop-at-feet", true);
        auctionService.returnAll(player.getUniqueId(), listings -> {
            if (listings.isEmpty()) {
                messages.send(player, "auctionhouse.return.empty");
                return;
            }
            List<Long> ids = new ArrayList<>();
            for (var listing : listings) {
                giveItem(player, listing.item().clone(), drop);
                ids.add(listing.id());
            }
            auctionService.markReturned(ids, count -> {
                if (count > 0) {
                    messages.send(player, "auctionhouse.returned", Map.of("count", String.valueOf(count)));
                } else {
                    messages.send(player, "auctionhouse.errors.return-failed");
                }
            });
        });
    }

    private String formatPrice(double price) {
        return String.format(Locale.ROOT, "%.2f", price);
    }

    private void giveItem(Player player, ItemStack item, boolean drop) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (leftover.isEmpty()) {
            return;
        }
        if (drop) {
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("open", "menu", "show", "reload", "help", "search", "sell", "list", "ulist",
                    "selling", "sold", "expired", "cancel", "return");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sold")) {
            return List.of("clear");
        }
        return List.of();
    }
}
