package me.moontimer.smpcore.auction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.moontimer.smpcore.SmpCorePlugin;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.util.DurationFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class AuctionMenuService {
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_PREV = "prev";
    private static final String ACTION_SORT = "sort";
    private static final String ACTION_SELLING = "selling";
    private static final String ACTION_SOLD = "sold";
    private static final String ACTION_EXPIRED = "expired";
    private static final String ACTION_BACK = "back";
    private static final String ACTION_CLEAR_SOLD = "clear_sold";
    private static final String ACTION_BUY = "buy";
    private static final String ACTION_CANCEL = "cancel";
    private static final String ACTION_RETURN = "return";
    private static final String ACTION_CONFIRM_LIST = "confirm_list";
    private static final String ACTION_CANCEL_LIST = "cancel_list";
    private static final String SERVER_SELLER_NAME = "Server";

    private final SmpCorePlugin plugin;
    private final MessageService messages;
    private final AuctionService auctionService;
    private final AuctionItemFactory itemFactory;
    private final NamespacedKey actionKey;
    private final NamespacedKey idKey;
    private final Map<UUID, PendingListing> pendingListings = new HashMap<>();
    private final Map<UUID, Map<Long, AuctionListing>> listingCache = new HashMap<>();

    public AuctionMenuService(SmpCorePlugin plugin, MessageService messages, AuctionService auctionService) {
        this.plugin = plugin;
        this.messages = messages;
        this.auctionService = auctionService;
        this.itemFactory = new AuctionItemFactory(plugin);
        this.actionKey = new NamespacedKey(plugin, "ah-action");
        this.idKey = new NamespacedKey(plugin, "ah-id");
    }

    public void openBrowse(Player player, int page, boolean showBack) {
        openListingsMenu(player, AuctionMenuType.BROWSE, null, page, getDefaultSort(), showBack);
    }

    public void openSearch(Player player, String filter, int page, boolean showBack) {
        openListingsMenu(player, AuctionMenuType.SEARCH, filter, page, getDefaultSort(), showBack);
    }

    public void openSelling(Player player, int page, boolean showBack) {
        openListingsMenu(player, AuctionMenuType.SELLING, null, page, getDefaultSort(), showBack);
    }

    public void openSold(Player player, int page, boolean showBack) {
        openListingsMenu(player, AuctionMenuType.SOLD, null, page, getDefaultSort(), showBack);
    }

    public void openExpired(Player player, int page, boolean showBack) {
        openListingsMenu(player, AuctionMenuType.EXPIRED, null, page, getDefaultSort(), showBack);
    }

    public void beginListing(Player player, PendingListing pending) {
        if (pending == null) {
            return;
        }
        if (pending.fee() > 0.0) {
            openConfirmListing(player, pending);
        } else {
            finalizeListing(player, pending);
        }
    }

    public void handleClick(Player player, AuctionMenuHolder holder, ItemStack item) {
        String action = getAction(item);
        if (action == null || action.isEmpty()) {
            return;
        }
        switch (action) {
            case ACTION_NEXT -> openListingsMenu(player, holder.getType(), holder.getFilter(),
                    holder.getPage() + 1, holder.getSort(), holder.isShowBack());
            case ACTION_PREV -> openListingsMenu(player, holder.getType(), holder.getFilter(),
                    holder.getPage() - 1, holder.getSort(), holder.isShowBack());
            case ACTION_SORT -> toggleSort(player, holder);
            case ACTION_SELLING -> openSelling(player, 0, holder.isShowBack());
            case ACTION_SOLD -> openSold(player, 0, holder.isShowBack());
            case ACTION_EXPIRED -> openExpired(player, 0, holder.isShowBack());
            case ACTION_BACK -> handleBack(player, holder);
            case ACTION_CLEAR_SOLD -> clearSold(player, holder);
            case ACTION_BUY -> handleBuy(player, item);
            case ACTION_CANCEL -> handleCancel(player, item, holder);
            case ACTION_RETURN -> handleReturn(player, item, holder);
            case ACTION_CONFIRM_LIST -> confirmListing(player);
            case ACTION_CANCEL_LIST -> cancelListing(player);
            default -> {
            }
        }
    }

    private void openListingsMenu(Player player, AuctionMenuType type, String filter, int page,
                                  AuctionSort sort, boolean showBack) {
        int size = getMenuSize();
        int pageSize = size - 9;
        if (type == AuctionMenuType.SELLING) {
            auctionService.listSellerActive(player.getUniqueId(), page, pageSize,
                    result -> openListingInventory(player, type, filter, sort, showBack, result));
            return;
        }
        if (type == AuctionMenuType.SOLD) {
            auctionService.listSellerSold(player.getUniqueId(), page, pageSize,
                    result -> openListingInventory(player, type, filter, sort, showBack, result));
            return;
        }
        if (type == AuctionMenuType.EXPIRED) {
            auctionService.listSellerExpired(player.getUniqueId(), page, pageSize,
                    result -> openListingInventory(player, type, filter, sort, showBack, result));
            return;
        }
        auctionService.listActive(filter, sort, page, pageSize,
                result -> openListingInventory(player, type, filter, sort, showBack, result));
    }

    private void openListingInventory(Player player, AuctionMenuType type, String filter, AuctionSort sort,
                                      boolean showBack, AuctionPage page) {
        if (!player.isOnline()) {
            return;
        }
        String title = getTitle(type, filter);
        AuctionMenuHolder holder = new AuctionMenuHolder(type, page.page(), sort, filter, showBack);
        Inventory inventory = Bukkit.createInventory(holder, getMenuSize(), title);
        holder.setInventory(inventory);
        fill(inventory);

        Map<Long, AuctionListing> cache = new HashMap<>();
        int slot = 0;
        int maxItems = inventory.getSize() - 9;
        for (AuctionListing listing : page.listings()) {
            if (slot >= maxItems) {
                break;
            }
            ItemStack display = buildListingItem(listing, type);
            inventory.setItem(slot++, display);
            cache.put(listing.id(), listing);
        }
        listingCache.put(player.getUniqueId(), cache);
        placeNavigation(player, holder, page);
        player.openInventory(inventory);
        playSound(player, "open");
    }

    private void openConfirmListing(Player player, PendingListing pending) {
        pendingListings.put(player.getUniqueId(), pending);
        AuctionMenuHolder holder = new AuctionMenuHolder(AuctionMenuType.CONFIRM_LIST, 0, getDefaultSort(),
                null, false);
        Inventory inventory = Bukkit.createInventory(holder, 27, getTitle(AuctionMenuType.CONFIRM_LIST, null));
        holder.setInventory(inventory);
        fill(inventory);

        ItemStack item = pending.item().clone();
        inventory.setItem(13, item);

        Map<String, String> placeholders = Map.of(
                "price", formatPrice(pending.price()),
                "fee", formatPrice(pending.fee())
        );
        ItemStack confirm = createButton(player, "confirm", "auctionhouse.menu.buttons.confirm.name",
                messages.formatList("auctionhouse.menu.buttons.confirm.lore", placeholders), ACTION_CONFIRM_LIST);
        ItemStack cancel = createButton(player, "cancel", "auctionhouse.menu.buttons.cancel.name",
                messages.getList("auctionhouse.menu.buttons.cancel.lore"), ACTION_CANCEL_LIST);
        inventory.setItem(11, confirm);
        inventory.setItem(15, cancel);

        player.openInventory(inventory);
        playSound(player, "open");
    }

    private void confirmListing(Player player) {
        PendingListing pending = pendingListings.remove(player.getUniqueId());
        if (pending == null) {
            player.closeInventory();
            return;
        }
        finalizeListing(player, pending);
    }

    private void cancelListing(Player player) {
        pendingListings.remove(player.getUniqueId());
        player.closeInventory();
    }

    private void finalizeListing(Player player, PendingListing pending) {
        boolean removeItem = pending.removeItem();
        if (removeItem && !takeFromHand(player, pending.item())) {
            messages.send(player, "auctionhouse.errors.item-changed");
            playSound(player, "error");
            return;
        }
        UUID sellerUuid = pending.serverListing() ? null : player.getUniqueId();
        String sellerName = pending.serverListing() ? SERVER_SELLER_NAME : player.getName();
        AuctionService.AuctionCreateRequest request = new AuctionService.AuctionCreateRequest(
                sellerUuid,
                sellerName,
                pending.item().clone(),
                pending.price(),
                pending.serverListing(),
                pending.unlimited(),
                pending.stock(),
                pending.fee()
        );
        auctionService.createListing(request, result -> {
            if (!result.success()) {
                if (removeItem) {
                    boolean drop = plugin.getConfig().getBoolean("auctionhouse.return.drop-at-feet", true);
                    giveItem(player, pending.item().clone(), drop);
                }
                String key = result.messageKey() == null ? "auctionhouse.errors.create-failed" : result.messageKey();
                if ("auctionhouse.errors.price-range".equals(key)) {
                    messages.send(player, key, Map.of(
                            "min", formatPrice(auctionService.getMinPrice()),
                            "max", formatPrice(auctionService.getMaxPrice())
                    ));
                } else if (result.reason() != null && !result.reason().isEmpty()) {
                    messages.send(player, key, Map.of("reason", result.reason()));
                } else if ("auctionhouse.errors.restricted".equals(key)) {
                    messages.send(player, key, Map.of("reason", "-"));
                } else {
                    messages.send(player, key);
                }
                playSound(player, "error");
                return;
            }
            messages.send(player, "auctionhouse.listing.created", Map.of(
                    "price", formatPrice(pending.price())
            ));
            if (pending.fee() > 0.0 && !pending.serverListing()) {
                messages.send(player, "auctionhouse.listing.fee", Map.of(
                        "fee", formatPrice(pending.fee())
                ));
            }
            if (auctionService.isAnnounceListings()) {
                Bukkit.broadcastMessage(messages.get("prefix") + messages.format("auctionhouse.announce.listing", Map.of(
                        "seller", sellerName,
                        "price", formatPrice(pending.price()),
                        "item", getItemName(pending.item())
                )));
            }
            playSound(player, "list");
        });
    }

    private void toggleSort(Player player, AuctionMenuHolder holder) {
        AuctionSort next = holder.getSort() == AuctionSort.TIME ? AuctionSort.PRICE : AuctionSort.TIME;
        openListingsMenu(player, holder.getType(), holder.getFilter(), 0, next, holder.isShowBack());
    }

    private void handleBack(Player player, AuctionMenuHolder holder) {
        String command = plugin.getConfig().getString("auctionhouse.menu.back-command", "");
        if (command == null || command.isEmpty()) {
            player.closeInventory();
            return;
        }
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(command));
    }

    private void clearSold(Player player, AuctionMenuHolder holder) {
        auctionService.clearSold(player.getUniqueId(), count -> {
            if (count > 0) {
                messages.send(player, "auctionhouse.sold.cleared", Map.of("count", String.valueOf(count)));
            } else {
                messages.send(player, "auctionhouse.sold.empty");
            }
            openListingsMenu(player, holder.getType(), holder.getFilter(), 0, holder.getSort(), holder.isShowBack());
        });
    }

    private void handleBuy(Player player, ItemStack item) {
        Long listingId = getListingId(item);
        if (listingId == null) {
            return;
        }
        auctionService.purchaseListing(player.getUniqueId(), player.getName(), listingId, result -> {
            if (!result.success()) {
                messages.send(player, result.messageKey());
                playSound(player, "error");
                return;
            }
            boolean stored = giveItem(player, result.item(), true);
            messages.send(player, "auctionhouse.buy.success", Map.of(
                    "price", formatPrice(result.price()),
                    "seller", result.sellerName() == null ? SERVER_SELLER_NAME : result.sellerName()
            ));
            if (!stored) {
                messages.send(player, "auctionhouse.errors.inventory-full");
            }
            if (result.sellerUuid() != null) {
                Player seller = Bukkit.getPlayer(result.sellerUuid());
                if (seller != null && seller.isOnline()) {
                    messages.send(seller, "auctionhouse.sell.success", Map.of(
                            "buyer", player.getName(),
                            "price", formatPrice(result.price()),
                            "tax", formatPrice(result.tax())
                    ));
                }
            }
            playSound(player, "buy");
        });
    }

    private void handleCancel(Player player, ItemStack item, AuctionMenuHolder holder) {
        Long listingId = getListingId(item);
        if (listingId == null) {
            return;
        }
        auctionService.cancelListing(player.getUniqueId(), listingId, success -> {
            if (success) {
                messages.send(player, "auctionhouse.cancelled");
            } else {
                messages.send(player, "auctionhouse.errors.cancel-failed");
            }
            openListingsMenu(player, holder.getType(), holder.getFilter(), holder.getPage(),
                    holder.getSort(), holder.isShowBack());
        });
    }

    private void handleReturn(Player player, ItemStack item, AuctionMenuHolder holder) {
        Long listingId = getListingId(item);
        if (listingId == null) {
            return;
        }
        AuctionListing listing = getCachedListing(player.getUniqueId(), listingId);
        if (listing == null) {
            return;
        }
        boolean drop = plugin.getConfig().getBoolean("auctionhouse.return.drop-at-feet", true);
        boolean stored = giveItem(player, listing.item().clone(), drop);
        auctionService.markReturned(List.of(listingId), count -> {
            if (count > 0) {
                messages.send(player, "auctionhouse.returned", Map.of("count", "1"));
            } else {
                messages.send(player, "auctionhouse.errors.return-failed");
            }
            openListingsMenu(player, holder.getType(), holder.getFilter(), holder.getPage(),
                    holder.getSort(), holder.isShowBack());
        });
        if (!stored) {
            messages.send(player, "auctionhouse.errors.inventory-full");
        }
    }

    private AuctionSort getDefaultSort() {
        String value = plugin.getConfig().getString("auctionhouse.menu.default-sort", "time");
        return AuctionSort.fromConfig(value);
    }

    private int getMenuSize() {
        int size = plugin.getConfig().getInt("auctionhouse.menu.size", 54);
        size = Math.max(27, Math.min(54, (size / 9) * 9));
        if (size < 27) {
            size = 27;
        }
        return size;
    }

    private void placeNavigation(Player player, AuctionMenuHolder holder, AuctionPage page) {
        Inventory inventory = holder.getInventory();
        int size = inventory.getSize();
        int rowStart = size - 9;
        int current = page.page() + 1;
        int total = page.totalPages();

        if (page.page() > 0) {
            inventory.setItem(rowStart, createButton(player, "previous",
                    "auctionhouse.menu.buttons.previous.name",
                    messages.formatList("auctionhouse.menu.buttons.previous.lore",
                            Map.of("page", String.valueOf(current - 1), "pages", String.valueOf(total))),
                    ACTION_PREV));
        }
        if (page.page() < total - 1) {
            inventory.setItem(rowStart + 8, createButton(player, "next",
                    "auctionhouse.menu.buttons.next.name",
                    messages.formatList("auctionhouse.menu.buttons.next.lore",
                            Map.of("page", String.valueOf(current + 1), "pages", String.valueOf(total))),
                    ACTION_NEXT));
        }
        inventory.setItem(rowStart + 1, createButton(player, "selling",
                "auctionhouse.menu.buttons.selling.name",
                messages.getList("auctionhouse.menu.buttons.selling.lore"), ACTION_SELLING));
        inventory.setItem(rowStart + 2, createButton(player, "sold",
                "auctionhouse.menu.buttons.sold.name",
                messages.getList("auctionhouse.menu.buttons.sold.lore"), ACTION_SOLD));
        inventory.setItem(rowStart + 3, createButton(player, "expired",
                "auctionhouse.menu.buttons.expired.name",
                messages.getList("auctionhouse.menu.buttons.expired.lore"), ACTION_EXPIRED));
        inventory.setItem(rowStart + 4, createButton(player, "sort",
                "auctionhouse.menu.buttons.sort.name",
                messages.formatList("auctionhouse.menu.buttons.sort.lore",
                        Map.of("sort", getSortName(holder.getSort()))), ACTION_SORT));
        if (holder.getType() == AuctionMenuType.SOLD) {
            inventory.setItem(rowStart + 6, createButton(player, "clear",
                    "auctionhouse.menu.buttons.clear.name",
                    messages.getList("auctionhouse.menu.buttons.clear.lore"), ACTION_CLEAR_SOLD));
        }
        if (holder.isShowBack()) {
            inventory.setItem(rowStart + 7, createButton(player, "back",
                    "auctionhouse.menu.buttons.back.name",
                    messages.getList("auctionhouse.menu.buttons.back.lore"), ACTION_BACK));
        }
    }

    private String getTitle(AuctionMenuType type, String filter) {
        return switch (type) {
            case SEARCH -> messages.format("auctionhouse.menu.search-title", Map.of(
                    "filter", filter == null ? "" : filter));
            case SELLING -> messages.get("auctionhouse.menu.selling-title");
            case SOLD -> messages.get("auctionhouse.menu.sold-title");
            case EXPIRED -> messages.get("auctionhouse.menu.expired-title");
            case CONFIRM_LIST -> messages.get("auctionhouse.menu.confirm-title");
            default -> messages.get("auctionhouse.menu.title");
        };
    }

    private ItemStack buildListingItem(AuctionListing listing, AuctionMenuType type) {
        ItemStack item = listing.item().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            lore.addAll(meta.getLore());
        }
        lore.add(" ");
        lore.add(messages.format("auctionhouse.menu.listing.price",
                Map.of("price", formatPrice(listing.price()))));
        if (type == AuctionMenuType.SOLD) {
            String buyer = listing.buyerName() == null ? "-" : listing.buyerName();
            lore.add(messages.format("auctionhouse.menu.listing.buyer", Map.of("buyer", buyer)));
            String soldAt = listing.soldAt() == null ? "-" : me.moontimer.smpcore.util.TimeUtil.formatTimestamp(listing.soldAt());
            lore.add(messages.format("auctionhouse.menu.listing.sold-at", Map.of("time", soldAt)));
        } else {
            String seller = listing.sellerName() == null ? SERVER_SELLER_NAME : listing.sellerName();
            lore.add(messages.format("auctionhouse.menu.listing.seller",
                    Map.of("seller", seller)));
            lore.add(messages.format("auctionhouse.menu.listing.time-left",
                    Map.of("time", formatRemaining(listing.expiresAt()))));
        }
        if (!listing.item().getEnchantments().isEmpty()) {
            lore.add(messages.get("auctionhouse.menu.listing.enchants"));
            listing.item().getEnchantments().forEach((enchant, level) -> lore.add(
                    messages.format("auctionhouse.menu.listing.enchant-line", Map.of(
                            "enchant", formatEnchantName(enchant.getKey().getKey()),
                            "level", String.valueOf(level)
                    ))));
        }
        if (meta != null) {
            meta.setLore(colorize(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if (type == AuctionMenuType.BROWSE || type == AuctionMenuType.SEARCH) {
                setAction(meta, ACTION_BUY, listing.id());
            } else if (type == AuctionMenuType.SELLING) {
                setAction(meta, ACTION_CANCEL, listing.id());
            } else if (type == AuctionMenuType.EXPIRED) {
                setAction(meta, ACTION_RETURN, listing.id());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getSortName(AuctionSort sort) {
        return switch (sort) {
            case PRICE -> messages.get("auctionhouse.sort.price");
            default -> messages.get("auctionhouse.sort.time");
        };
    }

    private String formatRemaining(Long expiresAt) {
        if (expiresAt == null) {
            return messages.get("auctionhouse.time.unlimited");
        }
        long seconds = Math.max(0L, (expiresAt - System.currentTimeMillis()) / 1000L);
        return DurationFormatter.formatSeconds(seconds);
    }

    private String formatPrice(double price) {
        return String.format(Locale.ROOT, "%.2f", price);
    }

    private String getItemName(ItemStack item) {
        if (item == null) {
            return "-";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return formatEnchantName(item.getType().name());
    }

    private String formatEnchantName(String name) {
        if (name == null || name.isEmpty()) {
            return "-";
        }
        String[] parts = name.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private void fill(Inventory inventory) {
        ItemStack filler = createFiller();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack createFiller() {
        String spec = plugin.getConfig().getString("auctionhouse.menu.filler", "GRAY_STAINED_GLASS_PANE");
        ItemStack item = itemFactory.createItem(spec, null);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createButton(Player viewer, String key, String namePath, List<String> lore, String action) {
        String materialSpec = plugin.getConfig().getString("auctionhouse.menu.buttons." + key + ".material", "STONE");
        ItemStack item = itemFactory.createItem(materialSpec, viewer);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (namePath != null) {
                meta.setDisplayName(messages.get(namePath));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(colorize(lore));
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            setAction(meta, action, null);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setAction(ItemMeta meta, String action, Long id) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(actionKey, PersistentDataType.STRING, action);
        if (id != null) {
            container.set(idKey, PersistentDataType.LONG, id);
        }
    }

    private String getAction(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    private Long getListingId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(idKey, PersistentDataType.LONG);
    }

    private AuctionListing getCachedListing(UUID viewer, long listingId) {
        Map<Long, AuctionListing> cache = listingCache.get(viewer);
        if (cache == null) {
            return null;
        }
        return cache.get(listingId);
    }

    private boolean takeFromHand(Player player, ItemStack item) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return false;
        }
        if (!hand.isSimilar(item) || hand.getAmount() < item.getAmount()) {
            return false;
        }
        int remaining = hand.getAmount() - item.getAmount();
        if (remaining <= 0) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            hand.setAmount(remaining);
            player.getInventory().setItemInMainHand(hand);
        }
        return true;
    }

    private boolean giveItem(Player player, ItemStack item, boolean drop) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (leftover.isEmpty()) {
            return true;
        }
        if (drop) {
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
        return false;
    }

    private List<String> colorize(List<String> lines) {
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(messages.colorize(line));
        }
        return result;
    }

    private void playSound(Player player, String key) {
        FileConfiguration config = plugin.getConfig();
        String base = "auctionhouse.sounds." + key;
        String soundName = config.getString(base + ".sound", config.getString(base, ""));
        if (soundName == null || soundName.isEmpty() || soundName.equalsIgnoreCase("none")) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
            float volume = (float) config.getDouble(base + ".volume", 1.0);
            float pitch = (float) config.getDouble(base + ".pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown sound: " + soundName);
        }
    }

    public record PendingListing(ItemStack item, double price, boolean serverListing, boolean unlimited,
                                 int stock, double fee, boolean removeItem) {
    }
}
