package me.moontimer.smpcore.auction;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import me.moontimer.smpcore.SmpCorePlugin;
import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.db.DatabaseManager;
import me.moontimer.smpcore.vault.VaultEconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AuctionService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SOLD = "SOLD";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_RETURNED = "RETURNED";

    private final SmpCorePlugin plugin;
    private final DatabaseManager database;
    private final VaultEconomyService vaultEconomy;
    private final AuditService audit;
    private final AuctionLogService logService;
    private final AuctionRestrictionService restrictions;

    private double minPrice;
    private double maxPrice;
    private double listingFeeFlat;
    private double listingFeePercent;
    private double saleTaxPercent;
    private double saleTaxCap;
    private long listingDurationMillis;
    private boolean expireServerListings;
    private boolean announceListings;
    private UUID serverAccount;
    private boolean cleanupEnabled;
    private long cleanupRetentionMillis;
    private long cleanupIntervalTicks;
    private int cleanupTaskId = -1;

    public AuctionService(SmpCorePlugin plugin, DatabaseManager database, VaultEconomyService vaultEconomy,
                          AuditService audit) {
        this.plugin = plugin;
        this.database = database;
        this.vaultEconomy = vaultEconomy;
        this.audit = audit;
        this.logService = new AuctionLogService(plugin);
        this.restrictions = new AuctionRestrictionService(plugin);
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        minPrice = Math.max(0.0, config.getDouble("auctionhouse.min-price", 1.0));
        maxPrice = Math.max(minPrice, config.getDouble("auctionhouse.max-price", 1000000.0));
        listingFeeFlat = Math.max(0.0, config.getDouble("auctionhouse.fees.listing.flat", 0.0));
        listingFeePercent = Math.max(0.0, config.getDouble("auctionhouse.fees.listing.percent", 0.0));
        saleTaxPercent = Math.max(0.0, config.getDouble("auctionhouse.fees.sale-tax.percent", 0.0));
        saleTaxCap = Math.max(0.0, config.getDouble("auctionhouse.fees.sale-tax.cap", 0.0));
        long hours = Math.max(0L, config.getLong("auctionhouse.listing-duration-hours", 72L));
        listingDurationMillis = hours <= 0L ? 0L : hours * 3600_000L;
        expireServerListings = config.getBoolean("auctionhouse.expire-server-listings", false);
        announceListings = config.getBoolean("auctionhouse.announce.enabled", false);
        String serverUuid = config.getString("auctionhouse.server-account-uuid",
                "00000000-0000-0000-0000-000000000000");
        serverAccount = parseUuid(serverUuid);
        cleanupEnabled = config.getBoolean("auctionhouse.cleanup.enabled", true);
        long retentionDays = Math.max(0L, config.getLong("auctionhouse.cleanup.retention-days", 7L));
        cleanupRetentionMillis = retentionDays * 86_400_000L;
        long cleanupIntervalSeconds = Math.max(30L, config.getLong("auctionhouse.cleanup.interval-seconds", 300L));
        cleanupIntervalTicks = cleanupIntervalSeconds * 20L;
        restrictions.reload();
        if (cleanupTaskId != -1) {
            stopCleanupTask();
            startCleanupTask();
        }
    }

    public void startCleanupTask() {
        if (!cleanupEnabled || cleanupIntervalTicks <= 0L) {
            return;
        }
        if (cleanupTaskId != -1) {
            return;
        }
        cleanupTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::runCleanup,
                cleanupIntervalTicks, cleanupIntervalTicks).getTaskId();
    }

    public void stopCleanupTask() {
        if (cleanupTaskId == -1) {
            return;
        }
        Bukkit.getScheduler().cancelTask(cleanupTaskId);
        cleanupTaskId = -1;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public boolean isAnnounceListings() {
        return announceListings;
    }

    public AuctionRestrictionService getRestrictions() {
        return restrictions;
    }

    public double calculateListingFee(double price) {
        double fee = listingFeeFlat + price * (listingFeePercent / 100.0);
        return Math.max(0.0, fee);
    }

    public double calculateSaleTax(double price) {
        double tax = price * (saleTaxPercent / 100.0);
        if (saleTaxCap > 0.0 && tax > saleTaxCap) {
            tax = saleTaxCap;
        }
        return Math.max(0.0, tax);
    }

    public long getListingDurationMillis(boolean serverListing) {
        if (serverListing && !expireServerListings) {
            return 0L;
        }
        return listingDurationMillis;
    }

    public void createListing(AuctionCreateRequest request, Consumer<AuctionCreateResult> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            AuctionCreateResult result = doCreateListing(request);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public void listActive(String filter, AuctionSort sort, int page, int pageSize,
                           Consumer<AuctionPage> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            AuctionPage result = queryActiveListings(filter, sort, page, pageSize);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public void listSellerActive(UUID seller, int page, int pageSize, Consumer<AuctionPage> callback) {
        listSellerByStatus(seller, List.of(STATUS_ACTIVE), page, pageSize, false, callback);
    }

    public void listSellerSold(UUID seller, int page, int pageSize, Consumer<AuctionPage> callback) {
        listSellerByStatus(seller, List.of(STATUS_SOLD), page, pageSize, true, callback);
    }

    public void listSellerExpired(UUID seller, int page, int pageSize, Consumer<AuctionPage> callback) {
        listSellerByStatus(seller, List.of(STATUS_CANCELLED, STATUS_EXPIRED), page, pageSize, false, callback);
    }

    public void cancelListing(UUID seller, long listingId, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE auction_listings SET status=?, updated_at=? "
                                 + "WHERE id=? AND seller_uuid=? AND status=? AND server_listing=FALSE")) {
                statement.setString(1, STATUS_CANCELLED);
                statement.setLong(2, System.currentTimeMillis());
                statement.setLong(3, listingId);
                statement.setString(4, seller.toString());
                statement.setString(5, STATUS_ACTIVE);
                success = statement.executeUpdate() > 0;
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to cancel auction listing", ex);
            }
            if (success) {
                logService.log("listing_cancel", Map.of(
                        "id", listingId,
                        "seller", seller.toString()
                ));
                audit.log(seller, "auction_cancel", Map.of("id", listingId));
            }
            boolean finalSuccess = success;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalSuccess));
        });
    }

    public void cancelAll(UUID seller, Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int count = 0;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE auction_listings SET status=?, updated_at=? "
                                 + "WHERE seller_uuid=? AND status=? AND server_listing=FALSE")) {
                statement.setString(1, STATUS_CANCELLED);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, seller.toString());
                statement.setString(4, STATUS_ACTIVE);
                count = statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to cancel auction listings", ex);
            }
            if (count > 0) {
                logService.log("listing_cancel_all", Map.of(
                        "seller", seller.toString(),
                        "count", count
                ));
                audit.log(seller, "auction_cancel_all", Map.of("count", count));
            }
            int finalCount = count;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalCount));
        });
    }

    public void clearSold(UUID seller, Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int count = 0;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE auction_listings SET sold_cleared=TRUE, updated_at=? "
                                 + "WHERE seller_uuid=? AND status=? AND sold_cleared=FALSE")) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, seller.toString());
                statement.setString(3, STATUS_SOLD);
                count = statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to clear sold listings", ex);
            }
            int finalCount = count;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalCount));
        });
    }

    public void returnAll(UUID seller, Consumer<List<AuctionListing>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<AuctionListing> listings = new ArrayList<>();
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM auction_listings WHERE seller_uuid=? "
                                 + "AND status IN (?, ?) AND server_listing=FALSE")) {
                statement.setString(1, seller.toString());
                statement.setString(2, STATUS_CANCELLED);
                statement.setString(3, STATUS_EXPIRED);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        AuctionListing listing = mapListing(rs);
                        if (listing != null) {
                            listings.add(listing);
                        }
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to load returnable listings", ex);
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(listings));
        });
    }

    public void markReturned(List<Long> ids, Consumer<Integer> callback) {
        if (ids == null || ids.isEmpty()) {
            callback.accept(0);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int count = 0;
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) {
                    placeholders.append(",");
                }
                placeholders.append("?");
            }
            String sql = "UPDATE auction_listings SET status=?, updated_at=? WHERE id IN (" + placeholders + ")";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, STATUS_RETURNED);
                statement.setLong(2, System.currentTimeMillis());
                int index = 3;
                for (Long id : ids) {
                    statement.setLong(index++, id);
                }
                count = statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to mark listings as returned", ex);
            }
            if (count > 0) {
                logService.log("listing_return", Map.of(
                        "count", count,
                        "ids", ids
                ));
            }
            int finalCount = count;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalCount));
        });
    }

    public void purchaseListing(UUID buyer, String buyerName, long listingId, Consumer<AuctionPurchaseResult> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            AuctionPurchaseResult result = doPurchaseListing(buyer, buyerName, listingId);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    private AuctionCreateResult doCreateListing(AuctionCreateRequest request) {
        if (!isEconomyAvailable()) {
            return AuctionCreateResult.failure("auctionhouse.errors.economy-missing");
        }
        if (request.item() == null || request.item().getType() == Material.AIR) {
            return AuctionCreateResult.failure("auctionhouse.errors.no-item");
        }
        if (request.price() < minPrice || request.price() > maxPrice) {
            return AuctionCreateResult.failure("auctionhouse.errors.price-range");
        }
        AuctionRestrictionService.RestrictionResult restriction = restrictions.check(request.item());
        if (restriction != null && !restriction.allowed()) {
            return AuctionCreateResult.failure(restriction.reason(), "auctionhouse.errors.restricted");
        }
        if (request.stock() < 1 && !request.unlimited()) {
            return AuctionCreateResult.failure("auctionhouse.errors.invalid-stock");
        }
        Economy economy = vaultEconomy.getEconomy();
        double fee = request.listingFee();
        OfflinePlayer seller = null;
        boolean feeTaken = false;
        if (!request.serverListing() && fee > 0.0) {
            if (economy == null || request.sellerUuid() == null) {
                return AuctionCreateResult.failure("auctionhouse.errors.economy-missing");
            }
            seller = Bukkit.getOfflinePlayer(request.sellerUuid());
            if (!economy.has(seller, fee)) {
                return AuctionCreateResult.failure("auctionhouse.errors.insufficient-funds");
            }
            if (!withdraw(economy, seller, fee)) {
                return AuctionCreateResult.failure("auctionhouse.errors.insufficient-funds");
            }
            feeTaken = true;
        }
        String searchText = buildSearchText(request.item(), request.sellerName());
        long now = System.currentTimeMillis();
        Long expiresAt = null;
        long duration = getListingDurationMillis(request.serverListing());
        if (duration > 0L) {
            expiresAt = now + duration;
        }
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            String itemData = ItemSerializer.toBase64(request.item());
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO auction_listings "
                            + "(seller_uuid, seller_name, item_data, price, created_at, expires_at, status, "
                            + "server_listing, unlimited, stock, search_text, sold_cleared, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, request.sellerUuid() == null ? null : request.sellerUuid().toString());
                statement.setString(2, request.sellerName());
                statement.setString(3, itemData);
                statement.setDouble(4, request.price());
                statement.setLong(5, now);
                if (expiresAt == null) {
                    statement.setNull(6, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(6, expiresAt);
                }
                statement.setString(7, STATUS_ACTIVE);
                statement.setBoolean(8, request.serverListing());
                statement.setBoolean(9, request.unlimited());
                statement.setInt(10, request.stock());
                statement.setString(11, searchText);
                statement.setLong(12, now);
                statement.executeUpdate();
                long id = 0L;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        id = keys.getLong(1);
                    }
                }
                connection.commit();
                logService.log("listing_create", Map.of(
                        "id", id,
                        "seller", request.sellerName(),
                        "price", request.price(),
                        "server", request.serverListing(),
                        "stock", request.stock(),
                        "unlimited", request.unlimited()
                ));
                audit.log(request.sellerUuid(), "auction_create", Map.of(
                        "id", id,
                        "price", request.price()
                ));
                if (feeTaken && serverAccount != null && economy != null) {
                    OfflinePlayer serverPlayer = Bukkit.getOfflinePlayer(serverAccount);
                    if (!deposit(economy, serverPlayer, fee)) {
                        plugin.getLogger().warning("Failed to deposit auction listing fee to server account.");
                    }
                }
                return AuctionCreateResult.success(id, request.listingFee());
            }
        } catch (SQLException | IOException ex) {
            if (feeTaken && economy != null && seller != null) {
                refund(economy, seller, fee);
            }
            plugin.getLogger().log(Level.WARNING, "Failed to create auction listing", ex);
            return AuctionCreateResult.failure("auctionhouse.errors.create-failed");
        }
    }

    private AuctionPurchaseResult doPurchaseListing(UUID buyer, String buyerName, long listingId) {
        if (!isEconomyAvailable()) {
            return AuctionPurchaseResult.failure("auctionhouse.errors.economy-missing");
        }
        Economy economy = vaultEconomy.getEconomy();
        if (economy == null) {
            return AuctionPurchaseResult.failure("auctionhouse.errors.economy-missing");
        }
        OfflinePlayer buyerPlayer = Bukkit.getOfflinePlayer(buyer);
        boolean withdrew = false;
        double price = 0.0;
        double tax = 0.0;
        double payout = 0.0;
        UUID sellerUuid = null;
        String sellerName = null;
        boolean serverListing = false;
        ItemStack item = null;
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT * FROM auction_listings WHERE id=? FOR UPDATE")) {
                select.setLong(1, listingId);
                try (ResultSet rs = select.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return AuctionPurchaseResult.failure("auctionhouse.errors.not-found");
                    }
                    String status = rs.getString("status");
                    if (!STATUS_ACTIVE.equalsIgnoreCase(status)) {
                        connection.rollback();
                        return AuctionPurchaseResult.failure("auctionhouse.errors.not-available");
                    }
                    serverListing = rs.getBoolean("server_listing");
                    boolean unlimited = rs.getBoolean("unlimited");
                    int stock = rs.getInt("stock");
                    if (!unlimited && stock <= 0) {
                        connection.rollback();
                        return AuctionPurchaseResult.failure("auctionhouse.errors.not-available");
                    }
                    Long expiresAt = rs.getObject("expires_at") == null ? null : rs.getLong("expires_at");
                    long now = System.currentTimeMillis();
                    if (expiresAt != null && expiresAt <= now) {
                        markExpired(connection, listingId, now);
                        connection.commit();
                        return AuctionPurchaseResult.failure("auctionhouse.errors.expired");
                    }
                    sellerUuid = parseUuid(rs.getString("seller_uuid"));
                    sellerName = rs.getString("seller_name");
                    if (sellerUuid != null && sellerUuid.equals(buyer)) {
                        connection.rollback();
                        return AuctionPurchaseResult.failure("auctionhouse.errors.buy-own");
                    }
                    price = rs.getDouble("price");
                    item = ItemSerializer.fromBase64(rs.getString("item_data"));
                    if (!economy.has(buyerPlayer, price)) {
                        connection.rollback();
                        return AuctionPurchaseResult.failure("auctionhouse.errors.insufficient-funds");
                    }
                    EconomyResponse withdraw = economy.withdrawPlayer(buyerPlayer, price);
                    if (withdraw == null || !withdraw.transactionSuccess()) {
                        connection.rollback();
                        return AuctionPurchaseResult.failure("auctionhouse.errors.insufficient-funds");
                    }
                    withdrew = true;
                    if (sellerUuid != null) {
                        tax = calculateSaleTax(price);
                    }
                    payout = price - tax;
                    AuctionStatus newStatus = AuctionStatus.ACTIVE;
                    int newStock = stock;
                    boolean markSold = false;
                    if (serverListing) {
                        if (!unlimited) {
                            newStock = stock - 1;
                            if (newStock <= 0) {
                                newStatus = AuctionStatus.SOLD;
                                markSold = true;
                            }
                        }
                    } else {
                        newStatus = AuctionStatus.SOLD;
                        markSold = true;
                        newStock = 0;
                    }
                    if (!serverListing || !unlimited || markSold) {
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE auction_listings SET status=?, stock=?, buyer_uuid=?, buyer_name=?, "
                                        + "sold_at=?, updated_at=? WHERE id=?")) {
                            update.setString(1, newStatus.name());
                            update.setInt(2, newStock);
                            update.setString(3, buyer.toString());
                            update.setString(4, buyerName);
                            if (markSold) {
                                update.setLong(5, now);
                            } else {
                                update.setNull(5, java.sql.Types.BIGINT);
                            }
                            update.setLong(6, now);
                            update.setLong(7, listingId);
                            update.executeUpdate();
                        }
                    }
                    connection.commit();
                    logService.log("listing_purchase", Map.of(
                            "id", listingId,
                            "buyer", buyerName,
                            "seller", sellerName,
                            "price", price,
                            "tax", tax
                    ));
                    audit.log(buyer, "auction_purchase", Map.of(
                            "id", listingId,
                            "seller", sellerName,
                            "price", price
                    ));
                    if (sellerUuid != null) {
                        OfflinePlayer sellerPlayer = Bukkit.getOfflinePlayer(sellerUuid);
                        if (payout > 0.0 && !deposit(economy, sellerPlayer, payout)) {
                            plugin.getLogger().warning("Failed to deposit auction payout to seller.");
                        }
                        if (tax > 0.0 && serverAccount != null) {
                            OfflinePlayer serverPlayer = Bukkit.getOfflinePlayer(serverAccount);
                            if (!deposit(economy, serverPlayer, tax)) {
                                plugin.getLogger().warning("Failed to deposit auction tax to server account.");
                            }
                        }
                    } else if (serverAccount != null) {
                        OfflinePlayer serverPlayer = Bukkit.getOfflinePlayer(serverAccount);
                        if (!deposit(economy, serverPlayer, price)) {
                            plugin.getLogger().warning("Failed to deposit auction payout to server account.");
                        }
                    }
                    return AuctionPurchaseResult.success(item, price, tax, sellerUuid, sellerName, serverListing);
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException ex) {
            if (withdrew) {
                refund(economy, buyerPlayer, price);
            }
            plugin.getLogger().log(Level.WARNING, "Failed to purchase auction listing", ex);
            return AuctionPurchaseResult.failure("auctionhouse.errors.purchase-failed");
        }
    }

    private AuctionPage queryActiveListings(String filter, AuctionSort sort, int page, int pageSize) {
        List<String> tokens = tokenize(filter);
        StringBuilder where = new StringBuilder("status=? AND (unlimited=TRUE OR stock>0)");
        List<Object> params = new ArrayList<>();
        params.add(STATUS_ACTIVE);
        for (String token : tokens) {
            where.append(" AND search_text LIKE ?");
            params.add("%" + token + "%");
        }
        String order = sort == AuctionSort.PRICE ? "price ASC, created_at DESC" : "created_at DESC";
        return queryListings(where.toString(), params, order, page, pageSize);
    }

    private void listSellerByStatus(UUID seller, List<String> statuses, int page, int pageSize,
                                    boolean filterSoldCleared, Consumer<AuctionPage> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            StringBuilder where = new StringBuilder("seller_uuid=? AND server_listing=FALSE");
            List<Object> params = new ArrayList<>();
            params.add(seller.toString());
            if (statuses.size() == 1) {
                where.append(" AND status=?");
                params.add(statuses.getFirst());
            } else {
                where.append(" AND status IN (");
                for (int i = 0; i < statuses.size(); i++) {
                    if (i > 0) {
                        where.append(",");
                    }
                    where.append("?");
                    params.add(statuses.get(i));
                }
                where.append(")");
            }
            if (filterSoldCleared) {
                where.append(" AND sold_cleared=FALSE");
            }
            AuctionPage result = queryListings(where.toString(), params, "created_at DESC", page, pageSize);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    private AuctionPage queryListings(String where, List<Object> params, String order, int page, int pageSize) {
        int total = 0;
        List<AuctionListing> listings = new ArrayList<>();
        String countSql = "SELECT COUNT(*) FROM auction_listings WHERE " + where;
        try (Connection connection = database.getConnection();
             PreparedStatement countStmt = connection.prepareStatement(countSql)) {
            bindParams(countStmt, params);
            try (ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
            int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);
            int safePage = Math.max(0, Math.min(page, totalPages - 1));
            int offset = safePage * pageSize;
            String sql = "SELECT * FROM auction_listings WHERE " + where + " ORDER BY " + order
                    + " LIMIT ? OFFSET ?";
            List<Object> listParams = new ArrayList<>(params);
            listParams.add(pageSize);
            listParams.add(offset);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindParams(statement, listParams);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        AuctionListing listing = mapListing(rs);
                        if (listing != null) {
                            listings.add(listing);
                        }
                    }
                }
            }
            return new AuctionPage(listings, safePage, totalPages, total);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to query auction listings", ex);
            return new AuctionPage(List.of(), 0, 1, 0);
        }
    }

    private void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        int index = 1;
        for (Object param : params) {
            if (param instanceof Integer integer) {
                statement.setInt(index++, integer);
            } else if (param instanceof Long longValue) {
                statement.setLong(index++, longValue);
            } else if (param instanceof Double doubleValue) {
                statement.setDouble(index++, doubleValue);
            } else {
                statement.setString(index++, param == null ? null : param.toString());
            }
        }
    }

    private AuctionListing mapListing(ResultSet rs) {
        try {
            ItemStack item = ItemSerializer.fromBase64(rs.getString("item_data"));
            UUID seller = parseUuid(rs.getString("seller_uuid"));
            UUID buyer = parseUuid(rs.getString("buyer_uuid"));
            Long expiresAt = rs.getObject("expires_at") == null ? null : rs.getLong("expires_at");
            Long soldAt = rs.getObject("sold_at") == null ? null : rs.getLong("sold_at");
            AuctionStatus status = AuctionStatus.valueOf(rs.getString("status").toUpperCase(Locale.ROOT));
            return new AuctionListing(
                    rs.getLong("id"),
                    seller,
                    rs.getString("seller_name"),
                    item,
                    rs.getDouble("price"),
                    rs.getLong("created_at"),
                    expiresAt,
                    status,
                    buyer,
                    rs.getString("buyer_name"),
                    soldAt,
                    rs.getBoolean("server_listing"),
                    rs.getBoolean("unlimited"),
                    rs.getInt("stock"),
                    rs.getBoolean("sold_cleared")
            );
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize auction item", ex);
            return null;
        }
    }

    private void runCleanup() {
        expireListings();
        cleanupOldListings();
    }

    private void expireListings() {
        long now = System.currentTimeMillis();
        String sql = "UPDATE auction_listings SET status=?, updated_at=? "
                + "WHERE status=? AND expires_at IS NOT NULL AND expires_at<=?";
        if (!expireServerListings) {
            sql += " AND server_listing=FALSE";
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, STATUS_EXPIRED);
            statement.setLong(2, now);
            statement.setString(3, STATUS_ACTIVE);
            statement.setLong(4, now);
            int updated = statement.executeUpdate();
            if (updated > 0) {
                logService.log("listing_expire", Map.of("count", updated));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to expire auction listings", ex);
        }
    }

    private void cleanupOldListings() {
        if (!cleanupEnabled || cleanupRetentionMillis <= 0L) {
            return;
        }
        long cutoff = System.currentTimeMillis() - cleanupRetentionMillis;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM auction_listings WHERE status IN (?, ?, ?) AND updated_at<=?")) {
            statement.setString(1, STATUS_EXPIRED);
            statement.setString(2, STATUS_CANCELLED);
            statement.setString(3, STATUS_RETURNED);
            statement.setLong(4, cutoff);
            int deleted = statement.executeUpdate();
            if (deleted > 0) {
                logService.log("listing_cleanup", Map.of("count", deleted));
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to cleanup auction listings", ex);
        }
    }

    private void markExpired(Connection connection, long listingId, long now) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE auction_listings SET status=?, updated_at=? WHERE id=?")) {
            update.setString(1, STATUS_EXPIRED);
            update.setLong(2, now);
            update.setLong(3, listingId);
            update.executeUpdate();
        }
        logService.log("listing_expire", Map.of("id", listingId));
    }

    private boolean isEconomyAvailable() {
        return vaultEconomy != null && vaultEconomy.isAvailable();
    }

    private boolean withdraw(Economy economy, OfflinePlayer player, double amount) {
        if (amount <= 0.0) {
            return true;
        }
        if (economy == null || player == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    private boolean deposit(Economy economy, OfflinePlayer player, double amount) {
        if (amount <= 0.0) {
            return true;
        }
        if (economy == null || player == null) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    private void refund(Economy economy, OfflinePlayer player, double amount) {
        if (!deposit(economy, player, amount)) {
            plugin.getLogger().warning("Failed to refund auction transaction.");
        }
    }

    private String buildSearchText(ItemStack item, String sellerName) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.getType().name().toLowerCase(Locale.ROOT)).append(' ');
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                sb.append(strip(meta.getDisplayName())).append(' ');
            }
            if (meta.hasLore() && meta.getLore() != null) {
                for (String line : meta.getLore()) {
                    sb.append(strip(line)).append(' ');
                }
            }
            if (!meta.getEnchants().isEmpty()) {
                meta.getEnchants().forEach((enchant, level) -> {
                    sb.append(enchant.getKey().getKey()).append(' ');
                    sb.append(enchant.getKey().toString()).append(' ');
                });
            }
        }
        if (sellerName != null) {
            sb.append(sellerName.toLowerCase(Locale.ROOT));
        }
        return sb.toString().trim();
    }

    private List<String> tokenize(String filter) {
        if (filter == null || filter.isBlank()) {
            return List.of();
        }
        String[] parts = filter.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private String strip(String value) {
        if (value == null) {
            return "";
        }
        return ChatColor.stripColor(value).toLowerCase(Locale.ROOT);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record AuctionCreateRequest(UUID sellerUuid, String sellerName, ItemStack item, double price,
                                       boolean serverListing, boolean unlimited, int stock, double listingFee) {
    }

    public record AuctionCreateResult(boolean success, String messageKey, String reason, long listingId,
                                      double feeCharged) {
        public static AuctionCreateResult success(long listingId, double feeCharged) {
            return new AuctionCreateResult(true, null, null, listingId, feeCharged);
        }

        public static AuctionCreateResult failure(String messageKey) {
            return new AuctionCreateResult(false, messageKey, null, 0L, 0.0);
        }

        public static AuctionCreateResult failure(String reason, String fallbackKey) {
            return new AuctionCreateResult(false, fallbackKey, reason, 0L, 0.0);
        }
    }

    public record AuctionPurchaseResult(boolean success, String messageKey, ItemStack item, double price,
                                        double tax, UUID sellerUuid, String sellerName, boolean serverListing) {
        public static AuctionPurchaseResult success(ItemStack item, double price, double tax,
                                                   UUID sellerUuid, String sellerName, boolean serverListing) {
            return new AuctionPurchaseResult(true, null, item, price, tax, sellerUuid, sellerName, serverListing);
        }

        public static AuctionPurchaseResult failure(String messageKey) {
            return new AuctionPurchaseResult(false, messageKey, null, 0.0, 0.0, null, null, false);
        }
    }
}
