package me.moontimer.smpcore.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import me.moontimer.smpcore.SmpCorePlugin;
import me.moontimer.smpcore.chat.MuteChatService;
import me.moontimer.smpcore.chat.SocialSpyService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.PlayerService;
import me.moontimer.smpcore.moderation.PunishmentRecord;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.moderation.PunishmentSummary;
import me.moontimer.smpcore.moderation.PunishmentType;
import me.moontimer.smpcore.staff.VanishService;
import me.moontimer.smpcore.util.DurationFormatter;
import me.moontimer.smpcore.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class StaffMenuService {
    private static final String ACTION_OPEN_MAIN = "open_main";
    private static final String ACTION_OPEN_PLAYERS = "open_players";
    private static final String ACTION_PAGE_PREV = "page_prev";
    private static final String ACTION_PAGE_NEXT = "page_next";
    private static final String ACTION_OPEN_PLAYER_PREFIX = "open_player:";
    private static final String ACTION_TOGGLE_VANISH = "toggle_vanish";
    private static final String ACTION_TOGGLE_FLY = "toggle_fly";
    private static final String ACTION_TOGGLE_SOCIALSPY = "toggle_socialspy";
    private static final String ACTION_TOGGLE_MUTECHAT = "toggle_mutechat";
    private static final String ACTION_CLEAR_CHAT = "clearchat";
    private static final String ACTION_OPEN_GM_SELF = "open_gm_self";
    private static final String ACTION_OPEN_GM_TARGET = "open_gm_target";
    private static final String ACTION_GM_PREFIX = "gm:";
    private static final String ACTION_BACK_PLAYER = "back_player";
    private static final String ACTION_TP_TO = "tp_to";
    private static final String ACTION_TP_HERE = "tp_here";
    private static final String ACTION_VTP = "vtp";
    private static final String ACTION_INFO = "info";
    private static final String ACTION_HISTORY = "history";
    private static final String ACTION_INVSEE = "invsee";
    private static final String ACTION_ENDERSEE = "endersee";
    private static final String ACTION_HEAL = "heal";
    private static final String ACTION_FEED = "feed";
    private static final String ACTION_FLY_TARGET = "fly_target";
    private static final String ACTION_OPEN_BAN_REASONS = "open_ban_reasons";
    private static final String ACTION_OPEN_WARN_REASONS = "open_warn_reasons";
    private static final String ACTION_BAN_REASON_PREFIX = "ban_reason:";
    private static final String ACTION_WARN_REASON_PREFIX = "warn_reason:";
    private static final String ACTION_MUTE_DEFAULT = "mute_default";
    private static final String ACTION_KICK_DEFAULT = "kick_default";
    private static final String ACTION_INFO_PAGE_PREV = "info_page_prev";
    private static final String ACTION_INFO_PAGE_NEXT = "info_page_next";
    private static final int[] INFO_HISTORY_SLOTS = {
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private final SmpCorePlugin plugin;
    private final MessageService messages;
    private final VanishService vanishService;
    private final SocialSpyService socialSpyService;
    private final MuteChatService muteChatService;
    private final PlayerService playerService;
    private final PunishmentService punishments;
    private final NamespacedKey actionKey;

    public StaffMenuService(SmpCorePlugin plugin, MessageService messages, VanishService vanishService,
                            SocialSpyService socialSpyService, MuteChatService muteChatService,
                            PlayerService playerService, PunishmentService punishments) {
        this.plugin = plugin;
        this.messages = messages;
        this.vanishService = vanishService;
        this.socialSpyService = socialSpyService;
        this.muteChatService = muteChatService;
        this.playerService = playerService;
        this.punishments = punishments;
        this.actionKey = new NamespacedKey(plugin, "menu-action");
    }

    public void openMainMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 45, colorize("&8Staff Menue"));
        holder.setInventory(inventory);
        fill(inventory);

        boolean vanished = vanishService.isVanished(player.getUniqueId());
        boolean flying = player.getAllowFlight();
        boolean spying = socialSpyService.isEnabled(player.getUniqueId());
        boolean muted = muteChatService.isMuted();

        inventory.setItem(10, createItem(Material.ENDER_EYE, "&bVanish",
                List.of(statusLine(vanished)), ACTION_TOGGLE_VANISH));
        inventory.setItem(11, createItem(Material.FEATHER, "&bFlug",
                List.of(statusLine(flying)), ACTION_TOGGLE_FLY));
        inventory.setItem(12, createItem(Material.SPYGLASS, "&bSocialSpy",
                List.of(statusLine(spying)), ACTION_TOGGLE_SOCIALSPY));
        inventory.setItem(13, createItem(Material.BARRIER, "&bMuteChat",
                List.of(statusLine(muted)), ACTION_TOGGLE_MUTECHAT));
        inventory.setItem(14, createItem(Material.PAPER, "&bChat leeren",
                List.of("&7Klicke zum Leeren"), ACTION_CLEAR_CHAT));
        inventory.setItem(15, createItem(Material.NETHER_STAR, "&bGamemode",
                List.of("&7Klicke fuer Auswahl"), ACTION_OPEN_GM_SELF));
        inventory.setItem(16, createItem(Material.PLAYER_HEAD, "&bSpieler",
                List.of("&7Klicke fuer Auswahl"), ACTION_OPEN_PLAYERS));

        player.openInventory(inventory);
    }

    public void openPlayerSelectMenu(Player player, int page) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int perPage = 45;
        int maxPage = Math.max(0, (players.size() - 1) / perPage);
        int clampedPage = Math.max(0, Math.min(page, maxPage));
        int start = clampedPage * perPage;
        int end = Math.min(players.size(), start + perPage);

        MenuHolder holder = new MenuHolder(MenuType.PLAYER_SELECT, null, clampedPage);
        Inventory inventory = Bukkit.createInventory(holder, 54, colorize("&8Spieler Menue"));
        holder.setInventory(inventory);
        fill(inventory);

        int slot = 0;
        for (int i = start; i < end; i++) {
            Player target = players.get(i);
            inventory.setItem(slot++, createPlayerItem(target));
        }

        if (clampedPage > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&7Zurueck", List.of("&7Seite " + clampedPage + "/" + (maxPage + 1)),
                    ACTION_PAGE_PREV));
        }
        inventory.setItem(49, createItem(Material.BARRIER, "&cHauptmenue", List.of("&7Zurueck"), ACTION_OPEN_MAIN));
        if (clampedPage < maxPage) {
            inventory.setItem(53, createItem(Material.ARROW, "&7Weiter", List.of("&7Seite " + (clampedPage + 2) + "/" + (maxPage + 1)),
                    ACTION_PAGE_NEXT));
        }

        player.openInventory(inventory);
    }

    public void openPlayerMenu(Player viewer, Player target) {
        MenuHolder holder = new MenuHolder(MenuType.PLAYER_ACTIONS, target.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 45, colorize("&8Staff -> &b" + target.getName()));
        holder.setInventory(inventory);
        fill(inventory);

        inventory.setItem(4, createPlayerHead(target));

        inventory.setItem(10, createItem(Material.ENDER_PEARL, "&bTP zu Spieler",
                List.of("&7Teleportiere dich"), ACTION_TP_TO));
        inventory.setItem(11, createItem(Material.ENDER_EYE, "&bTP hier",
                List.of("&7Spieler zu dir"), ACTION_TP_HERE));
        inventory.setItem(12, createItem(Material.ENDER_PEARL, "&bVanish TP",
                List.of("&7Vanish + Teleport"), ACTION_VTP));
        inventory.setItem(13, createItem(Material.BOOK, "&bInfo",
                List.of("&7Infos anzeigen"), ACTION_INFO));
        inventory.setItem(14, createItem(Material.PAPER, "&bHistory",
                List.of("&7Verlauf anzeigen"), ACTION_HISTORY));
        inventory.setItem(15, createItem(Material.CHEST, "&bInvsee",
                List.of("&7Inventar oeffnen"), ACTION_INVSEE));
        inventory.setItem(16, createItem(Material.ENDER_CHEST, "&bEndersee",
                List.of("&7Endertruhe oeffnen"), ACTION_ENDERSEE));

        inventory.setItem(19, createItem(Material.RED_DYE, "&cBan",
                List.of("&7Gruende auswaehlen"), ACTION_OPEN_BAN_REASONS));
        inventory.setItem(20, createItem(Material.YELLOW_DYE, "&eWarn",
                List.of("&7Gruende auswaehlen"), ACTION_OPEN_WARN_REASONS));
        inventory.setItem(21, createItem(Material.ORANGE_DYE, "&6Mute",
                List.of("&7Standardgrund"), ACTION_MUTE_DEFAULT));
        inventory.setItem(22, createItem(Material.IRON_BOOTS, "&cKick",
                List.of("&7Standardgrund"), ACTION_KICK_DEFAULT));
        inventory.setItem(23, createItem(Material.FEATHER, "&bFlug",
                List.of("&7Toggle Flug"), ACTION_FLY_TARGET));
        inventory.setItem(24, createItem(Material.GOLDEN_APPLE, "&aHeal",
                List.of("&7Heilen"), ACTION_HEAL));
        inventory.setItem(25, createItem(Material.BREAD, "&aFeed",
                List.of("&7Fuettern"), ACTION_FEED));
        inventory.setItem(26, createItem(Material.NETHER_STAR, "&bGamemode",
                List.of("&7Klicke fuer Auswahl"), ACTION_OPEN_GM_TARGET));

        inventory.setItem(40, createItem(Material.BARRIER, "&cHauptmenue",
                List.of("&7Zurueck"), ACTION_OPEN_MAIN));

        viewer.openInventory(inventory);
    }

    public void openInfoMenu(Player viewer, UUID targetId, String targetName, int page) {
        if (targetId == null) {
            messages.send(viewer, "errors.player-not-found");
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetId);
        String displayName = targetName;
        if (displayName == null || displayName.isEmpty()) {
            displayName = offline.getName();
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = targetId.toString();
        }
        String finalDisplayName = displayName;

        Player online = Bukkit.getPlayer(targetId);
        String onlineIp = online != null && online.getAddress() != null
                ? online.getAddress().getAddress().getHostAddress()
                : "";
        int safePage = Math.max(1, page);

        playerService.getPlayerInfo(targetId, info -> {
            if (!viewer.isOnline()) {
                return;
            }
            String ip = onlineIp;
            String first = "-";
            String last = "-";
            String playtime = "-";
            if (info != null) {
                if (ip.isEmpty() && info.ip() != null) {
                    ip = info.ip();
                }
                first = TimeUtil.formatTimestamp(info.firstJoin());
                last = TimeUtil.formatTimestamp(info.lastJoin());
                playtime = formatPlaytime(info.playtimeSeconds());
            }
            String finalIp = ip == null ? "" : ip;
            String finalFirst = first;
            String finalLast = last;
            String finalPlaytime = playtime;

            punishments.getSummary(targetId, summary -> {
                if (!viewer.isOnline()) {
                    return;
                }
                punishments.getActiveBan(targetId, finalIp, activeBan -> {
                    if (!viewer.isOnline()) {
                        return;
                    }
                    punishments.getActiveMute(targetId, activeMute -> {
                        if (!viewer.isOnline()) {
                            return;
                        }
                        int pageSize = INFO_HISTORY_SLOTS.length;
                        punishments.getHistory(targetId, safePage, pageSize, (records, totalPages) -> {
                            if (!viewer.isOnline()) {
                                return;
                            }
                            int clampedPage = Math.min(safePage, totalPages);
                            if (clampedPage != safePage) {
                                openLater(() -> openInfoMenu(viewer, targetId, finalDisplayName, clampedPage));
                                return;
                            }
                            openInfoInventory(viewer, offline, online, finalDisplayName, finalIp,
                                    finalFirst, finalLast, finalPlaytime, summary, activeBan, activeMute,
                                    records, totalPages, safePage);
                        });
                    });
                });
            });
        });
    }

    public void openBanReasons(Player viewer, Player target) {
        List<String> reasons = getReasons("staff-menu.ban-reasons");
        openReasonMenu(viewer, target, MenuType.BAN_REASONS, "&8Ban Gruende", reasons, ACTION_BAN_REASON_PREFIX);
    }

    public void openWarnReasons(Player viewer, Player target) {
        List<String> reasons = getReasons("staff-menu.warn-reasons");
        openReasonMenu(viewer, target, MenuType.WARN_REASONS, "&8Warn Gruende", reasons, ACTION_WARN_REASON_PREFIX);
    }

    public void openGamemodeMenu(Player viewer, Player target) {
        MenuHolder holder = new MenuHolder(MenuType.GAMEMODE, target.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, colorize("&8Gamemode"));
        holder.setInventory(inventory);
        fill(inventory);

        inventory.setItem(10, createItem(Material.GRASS_BLOCK, "&aSurvival",
                List.of("&7Klicke"), ACTION_GM_PREFIX + "survival"));
        inventory.setItem(12, createItem(Material.DIAMOND_BLOCK, "&bCreative",
                List.of("&7Klicke"), ACTION_GM_PREFIX + "creative"));
        inventory.setItem(14, createItem(Material.MAP, "&eAdventure",
                List.of("&7Klicke"), ACTION_GM_PREFIX + "adventure"));
        inventory.setItem(16, createItem(Material.ENDER_EYE, "&7Spectator",
                List.of("&7Klicke"), ACTION_GM_PREFIX + "spectator"));

        String backAction = target.getUniqueId().equals(viewer.getUniqueId()) ? ACTION_OPEN_MAIN : ACTION_BACK_PLAYER;
        inventory.setItem(22, createItem(Material.BARRIER, "&cZurueck",
                List.of("&7Zum Menue"), backAction));

        viewer.openInventory(inventory);
    }

    private void openInfoInventory(Player viewer, OfflinePlayer target, Player online, String displayName, String ip,
                                   String first, String last, String playtime, PunishmentSummary summary,
                                   PunishmentRecord activeBan, PunishmentRecord activeMute,
                                   List<PunishmentRecord> history, int totalPages, int page) {
        MenuHolder holder = new MenuHolder(MenuType.INFO, target.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(holder, 54, colorize("&8Info -> &b" + displayName));
        holder.setInventory(inventory);
        fill(inventory);

        List<String> headLore = new ArrayList<>();
        headLore.add("&7Status: " + (online != null ? "&aOnline" : "&cOffline"));
        if (online != null) {
            headLore.add("&7Welt: " + online.getWorld().getName());
        }
        headLore.add("&7UUID: " + target.getUniqueId());
        inventory.setItem(4, createPlayerHead(target, "&b" + displayName, headLore));

        String ipDisplay = ip == null || ip.isEmpty() ? "-" : ip;
        List<String> infoLore = new ArrayList<>();
        infoLore.add("&7UUID: &f" + target.getUniqueId());
        infoLore.add("&7IP: &f" + ipDisplay);
        infoLore.add("&7Erster Join: &f" + first);
        infoLore.add("&7Letzter Join: &f" + last);
        infoLore.add("&7Spielzeit: &f" + playtime);
        infoLore.add("&7Warnungen: &f" + summary.warnings());
        inventory.setItem(10, createItem(Material.BOOK, "&bSpieler Info", infoLore, null));

        inventory.setItem(12, createItem(getActiveMaterial(activeBan, PunishmentType.BAN), "&cAktiver Ban",
                buildActiveLore(activeBan), null));
        inventory.setItem(14, createItem(getActiveMaterial(activeMute, PunishmentType.MUTE), "&6Aktiver Mute",
                buildActiveLore(activeMute), null));

        List<String> lastLore = new ArrayList<>();
        lastLore.add("&7Letzter Ban: &f" + formatLastRecord(summary.lastBan()));
        lastLore.add("&7Letzter Mute: &f" + formatLastRecord(summary.lastMute()));
        lastLore.add("&7Letzte Warnung: &f" + formatLastWarn(summary.lastWarn()));
        inventory.setItem(16, createItem(Material.PAPER, "&bLetzte Strafen", lastLore, null));

        if (online != null && viewer.hasPermission("smpcore.staff.vtp")) {
            inventory.setItem(20, createItem(Material.ENDER_PEARL, "&bVanish TP",
                    List.of("&7Vanish + Teleport"), ACTION_VTP));
        }
        inventory.setItem(22, createItem(Material.MAP, "&bHistory",
                List.of("&7Seite " + page + "/" + totalPages), null));
        if (history.isEmpty()) {
            inventory.setItem(31, createItem(Material.BARRIER, "&7Kein Verlauf",
                    List.of("&7Keine Eintraege"), null));
        } else {
            int slotIndex = 0;
            for (PunishmentRecord record : history) {
                if (slotIndex >= INFO_HISTORY_SLOTS.length) {
                    break;
                }
                inventory.setItem(INFO_HISTORY_SLOTS[slotIndex++], createHistoryItem(record));
            }
        }

        if (page > 1) {
            inventory.setItem(45, createItem(Material.ARROW, "&7Zurueck",
                    List.of("&7Seite " + (page - 1)), ACTION_INFO_PAGE_PREV));
        }
        if (page < totalPages) {
            inventory.setItem(53, createItem(Material.ARROW, "&7Weiter",
                    List.of("&7Seite " + (page + 1)), ACTION_INFO_PAGE_NEXT));
        }

        String backAction = getBackAction(viewer, online);
        inventory.setItem(49, createItem(Material.BARRIER, "&cZurueck",
                getBackLore(viewer, online), backAction));

        viewer.openInventory(inventory);
    }

    public void handleClick(Player player, MenuHolder holder, ItemStack item) {
        String action = getAction(item);
        if (action == null || action.isEmpty()) {
            return;
        }
        if (action.startsWith(ACTION_OPEN_PLAYER_PREFIX)) {
            UUID targetId = parseUuid(action.substring(ACTION_OPEN_PLAYER_PREFIX.length()));
            if (targetId == null) {
                return;
            }
            Player target = Bukkit.getPlayer(targetId);
            if (target == null) {
                messages.send(player, "errors.player-not-found");
                return;
            }
            openLater(() -> openPlayerMenu(player, target));
            return;
        }
        switch (action) {
            case ACTION_OPEN_MAIN -> openLater(() -> openMainMenu(player));
            case ACTION_OPEN_PLAYERS -> openLater(() -> openPlayerSelectMenu(player, 0));
            case ACTION_PAGE_PREV -> openLater(() -> openPlayerSelectMenu(player, holder.getPage() - 1));
            case ACTION_PAGE_NEXT -> openLater(() -> openPlayerSelectMenu(player, holder.getPage() + 1));
            case ACTION_INFO_PAGE_PREV -> {
                if (holder.getTarget() != null) {
                    openLater(() -> openInfoMenu(player, holder.getTarget(), null, holder.getPage() - 1));
                }
            }
            case ACTION_INFO_PAGE_NEXT -> {
                if (holder.getTarget() != null) {
                    openLater(() -> openInfoMenu(player, holder.getTarget(), null, holder.getPage() + 1));
                }
            }
            case ACTION_TOGGLE_VANISH -> {
                player.performCommand("vanish");
                openLater(() -> openMainMenu(player));
            }
            case ACTION_TOGGLE_FLY -> {
                player.performCommand("fly");
                openLater(() -> openMainMenu(player));
            }
            case ACTION_TOGGLE_SOCIALSPY -> {
                player.performCommand("socialspy");
                openLater(() -> openMainMenu(player));
            }
            case ACTION_TOGGLE_MUTECHAT -> {
                player.performCommand("mutechat");
                openLater(() -> openMainMenu(player));
            }
            case ACTION_CLEAR_CHAT -> {
                player.performCommand("clearchat");
                openLater(() -> openMainMenu(player));
            }
            case ACTION_OPEN_GM_SELF -> openLater(() -> openGamemodeMenu(player, player));
            case ACTION_OPEN_GM_TARGET -> {
                Player target = getTarget(holder);
                if (target != null) {
                    openLater(() -> openGamemodeMenu(player, target));
                }
            }
            case ACTION_BACK_PLAYER -> {
                Player target = getTarget(holder);
                if (target != null) {
                    openLater(() -> openPlayerMenu(player, target));
                }
            }
            case ACTION_TP_TO -> runCommandOnTarget(player, holder, "tp");
            case ACTION_TP_HERE -> runCommandOnTarget(player, holder, "tphere");
            case ACTION_VTP -> runCommandOnTarget(player, holder, "vtp");
            case ACTION_INFO -> runCommandOnTarget(player, holder, "info");
            case ACTION_HISTORY -> runCommandOnTarget(player, holder, "history");
            case ACTION_INVSEE -> runCommandOnTarget(player, holder, "invsee");
            case ACTION_ENDERSEE -> runCommandOnTarget(player, holder, "endersee");
            case ACTION_HEAL -> runCommandOnTarget(player, holder, "heal");
            case ACTION_FEED -> runCommandOnTarget(player, holder, "feed");
            case ACTION_FLY_TARGET -> runCommandOnTarget(player, holder, "fly");
            case ACTION_OPEN_BAN_REASONS -> {
                Player target = getTarget(holder);
                if (target != null) {
                    openLater(() -> openBanReasons(player, target));
                }
            }
            case ACTION_OPEN_WARN_REASONS -> {
                Player target = getTarget(holder);
                if (target != null) {
                    openLater(() -> openWarnReasons(player, target));
                }
            }
            case ACTION_MUTE_DEFAULT -> runReasonCommand(player, holder, "mute", getDefaultReason());
            case ACTION_KICK_DEFAULT -> runReasonCommand(player, holder, "kick", getDefaultReason());
            default -> handleReasonAction(player, holder, action);
        }
    }

    private void handleReasonAction(Player player, MenuHolder holder, String action) {
        if (action.startsWith(ACTION_BAN_REASON_PREFIX)) {
            int index = parseIndex(action.substring(ACTION_BAN_REASON_PREFIX.length()));
            List<String> reasons = getReasons("staff-menu.ban-reasons");
            runReasonAtIndex(player, holder, "ban", reasons, index);
            return;
        }
        if (action.startsWith(ACTION_WARN_REASON_PREFIX)) {
            int index = parseIndex(action.substring(ACTION_WARN_REASON_PREFIX.length()));
            List<String> reasons = getReasons("staff-menu.warn-reasons");
            runReasonAtIndex(player, holder, "warn", reasons, index);
        }
        if (action.startsWith(ACTION_GM_PREFIX)) {
            String mode = action.substring(ACTION_GM_PREFIX.length()).toLowerCase(Locale.ROOT);
            Player target = getTarget(holder);
            if (target == null) {
                return;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.performCommand("gamemode " + mode);
            } else {
                player.performCommand("gamemode " + mode + " " + target.getName());
            }
            return;
        }
    }

    private void openReasonMenu(Player viewer, Player target, MenuType type, String title, List<String> reasons,
                                String actionPrefix) {
        int size = Math.min(54, Math.max(27, ((reasons.size() - 1) / 9 + 1) * 9));
        MenuHolder holder = new MenuHolder(type, target.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, size, colorize(title));
        holder.setInventory(inventory);
        fill(inventory);

        int slot = 0;
        for (int i = 0; i < reasons.size() && slot < size - 1; i++) {
            String reason = reasons.get(i);
            inventory.setItem(slot++, createItem(Material.PAPER, "&c" + reason,
                    List.of("&7Klicke zum Auswaehlen"), actionPrefix + i));
        }
        inventory.setItem(size - 1, createItem(Material.BARRIER, "&cZurueck",
                List.of("&7Zum Spieler-Menue"), ACTION_BACK_PLAYER));

        viewer.openInventory(inventory);
    }

    private void runCommandOnTarget(Player player, MenuHolder holder, String command) {
        Player target = getTarget(holder);
        if (target == null) {
            messages.send(player, "errors.player-not-found");
            return;
        }
        player.performCommand(command + " " + target.getName());
    }

    private void runReasonCommand(Player player, MenuHolder holder, String command, String reason) {
        Player target = getTarget(holder);
        if (target == null) {
            messages.send(player, "errors.player-not-found");
            return;
        }
        player.performCommand(command + " " + target.getName() + " " + reason);
        player.closeInventory();
    }

    private void runReasonAtIndex(Player player, MenuHolder holder, String command, List<String> reasons, int index) {
        if (index < 0 || index >= reasons.size()) {
            return;
        }
        runReasonCommand(player, holder, command, reasons.get(index));
        player.closeInventory();
    }

    private Player getTarget(MenuHolder holder) {
        if (holder.getTarget() == null) {
            return null;
        }
        return Bukkit.getPlayer(holder.getTarget());
    }

    private List<String> getReasons(String path) {
        List<String> reasons = new ArrayList<>(plugin.getConfig().getStringList(path));
        if (reasons.isEmpty()) {
            reasons.add(getDefaultReason());
        }
        return reasons;
    }

    private String getDefaultReason() {
        return plugin.getConfig().getString("punishments.default-reason", "Regelverstoss");
    }

    private String getBackAction(Player viewer, Player online) {
        if (online != null && viewer.hasPermission("smpcore.staff.menu")) {
            return ACTION_BACK_PLAYER;
        }
        if (viewer.hasPermission("smpcore.staff.menu")) {
            return ACTION_OPEN_MAIN;
        }
        return "";
    }

    private List<String> getBackLore(Player viewer, Player online) {
        if (online != null && viewer.hasPermission("smpcore.staff.menu")) {
            return List.of("&7Zum Spieler-Menue");
        }
        if (viewer.hasPermission("smpcore.staff.menu")) {
            return List.of("&7Zum Hauptmenue");
        }
        return List.of("&7Schliessen");
    }

    private List<String> buildActiveLore(PunishmentRecord record) {
        if (record == null || !record.isActive(System.currentTimeMillis())) {
            return List.of("&7Status: &cKEINER");
        }
        return List.of(
                "&7Status: &aAKTIV",
                "&7Grund: &f" + safeReason(record.reason()),
                "&7Ende: &f" + formatRemaining(record),
                "&7ID: &f" + record.id()
        );
    }

    private Material getActiveMaterial(PunishmentRecord record, PunishmentType type) {
        if (record == null || !record.isActive(System.currentTimeMillis())) {
            return Material.GRAY_DYE;
        }
        return type == PunishmentType.BAN ? Material.RED_DYE : Material.ORANGE_DYE;
    }

    private String formatLastRecord(PunishmentRecord record) {
        if (record == null) {
            return "-";
        }
        return safeReason(record.reason()) + " (" + getStatus(record) + ") " + TimeUtil.formatTimestamp(record.createdAt());
    }

    private String formatLastWarn(PunishmentRecord record) {
        if (record == null) {
            return "-";
        }
        return safeReason(record.reason()) + " " + TimeUtil.formatTimestamp(record.createdAt());
    }

    private ItemStack createHistoryItem(PunishmentRecord record) {
        Material material = getHistoryMaterial(record.type());
        String name = getHistoryColor(record.type()) + "#" + record.id() + " " + formatTypeName(record.type());
        List<String> lore = new ArrayList<>();
        lore.add("&7Grund: &f" + safeReason(record.reason()));
        lore.add("&7Erstellt: &f" + TimeUtil.formatTimestamp(record.createdAt()));
        lore.add("&7Ende: &f" + formatRemaining(record));
        lore.add("&7Status: &f" + getStatus(record));
        return createItem(material, name, lore, null);
    }

    private Material getHistoryMaterial(PunishmentType type) {
        return switch (type) {
            case BAN -> Material.RED_DYE;
            case MUTE -> Material.ORANGE_DYE;
            case WARN -> Material.YELLOW_DYE;
            case KICK -> Material.IRON_BOOTS;
        };
    }

    private String getHistoryColor(PunishmentType type) {
        return switch (type) {
            case BAN -> "&c";
            case MUTE -> "&6";
            case WARN -> "&e";
            case KICK -> "&7";
        };
    }

    private String formatTypeName(PunishmentType type) {
        return switch (type) {
            case BAN -> "Ban";
            case MUTE -> "Mute";
            case WARN -> "Warn";
            case KICK -> "Kick";
        };
    }

    private String formatRemaining(PunishmentRecord record) {
        if (record == null || record.expiresAt() == null) {
            return "permanent";
        }
        long remaining = Math.max(0L, (record.expiresAt() - System.currentTimeMillis()) / 1000L);
        return DurationFormatter.formatSeconds(remaining);
    }

    private String safeReason(String reason) {
        if (reason == null || reason.isEmpty()) {
            return "-";
        }
        return reason;
    }

    private String getStatus(PunishmentRecord record) {
        if (record.revokedAt() != null) {
            return "widerrufen";
        }
        if (record.expiresAt() != null && record.expiresAt() <= System.currentTimeMillis()) {
            return "abgelaufen";
        }
        return "aktiv";
    }

    private String formatPlaytime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format(Locale.ROOT, "%dh %dm", hours, minutes);
    }

    private String statusLine(boolean enabled) {
        return "&7Status: " + (enabled ? "&aAN" : "&cAUS");
    }

    private ItemStack createPlayerItem(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.setDisplayName(colorize("&b" + target.getName()));
        meta.setLore(colorize(List.of("&7Klicke fuer Aktionen")));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING,
                ACTION_OPEN_PLAYER_PREFIX + target.getUniqueId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerHead(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.setDisplayName(colorize("&b" + target.getName()));
        meta.setLore(colorize(List.of("&7Welt: " + target.getWorld().getName(),
                "&7UUID: " + target.getUniqueId().toString())));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerHead(OfflinePlayer target, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.setDisplayName(colorize(name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(colorize(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(colorize(lore));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (action != null && !action.isEmpty()) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(actionKey, PersistentDataType.STRING, action);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = createFiller();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private String getAction(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    private String colorize(String text) {
        return messages.colorize(text);
    }

    private List<String> colorize(List<String> lines) {
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(colorize(line));
        }
        return result;
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int parseIndex(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private void openLater(Runnable action) {
        Bukkit.getScheduler().runTask(plugin, action);
    }
}
