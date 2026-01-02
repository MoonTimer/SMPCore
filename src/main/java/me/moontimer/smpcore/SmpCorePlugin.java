package me.moontimer.smpcore;

import java.util.Objects;
import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.chat.ChatService;
import me.moontimer.smpcore.chat.IgnoreService;
import me.moontimer.smpcore.chat.MuteChatService;
import me.moontimer.smpcore.chat.SocialSpyService;
import me.moontimer.smpcore.combat.CombatService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.PlayerService;
import me.moontimer.smpcore.core.TablistService;
import me.moontimer.smpcore.db.DatabaseManager;
import me.moontimer.smpcore.economy.EconomyService;
import me.moontimer.smpcore.menu.StaffMenuService;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.rtp.RtpService;
import me.moontimer.smpcore.staff.VanishService;
import me.moontimer.smpcore.teleport.BackManager;
import me.moontimer.smpcore.teleport.CooldownManager;
import me.moontimer.smpcore.teleport.HomeService;
import me.moontimer.smpcore.teleport.TeleportManager;
import me.moontimer.smpcore.teleport.TpaService;
import me.moontimer.smpcore.teleport.WarmupManager;
import me.moontimer.smpcore.teleport.WarpService;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmpCorePlugin extends JavaPlugin {
    private static SmpCorePlugin instance;

    private MessageService messages;
    private DatabaseManager database;
    private AuditService audit;

    private CooldownManager cooldowns;
    private WarmupManager warmups;
    private BackManager backManager;
    private TeleportManager teleportManager;
    private TpaService tpaService;
    private HomeService homeService;
    private WarpService warpService;

    private IgnoreService ignoreService;
    private ChatService chatService;
    private SocialSpyService socialSpyService;
    private MuteChatService muteChatService;

    private PunishmentService punishmentService;
    private EconomyService economyService;
    private RtpService rtpService;
    private PlayerService playerService;
    private TablistService tablistService;
    private VanishService vanishService;
    private CombatService combatService;
    private StaffMenuService menuService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        messages = new MessageService(this);

        database = new DatabaseManager(this);
        if (!database.initialize()) {
            getLogger().severe("Database initialization failed; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        database.migrate();

        audit = new AuditService(this, database);
        cooldowns = new CooldownManager();
        warmups = new WarmupManager(this);
        backManager = new BackManager();
        teleportManager = new TeleportManager(this, messages, cooldowns, warmups, backManager, audit);
        tpaService = new TpaService(this, messages, teleportManager, audit);
        homeService = new HomeService(database, audit, this);
        warpService = new WarpService(database, audit, this);

        ignoreService = new IgnoreService(database, this);
        socialSpyService = new SocialSpyService();
        muteChatService = new MuteChatService();
        chatService = new ChatService(messages, ignoreService, socialSpyService, muteChatService);

        playerService = new PlayerService(this, database);
        punishmentService = new PunishmentService(this, database, audit, messages);
        economyService = new EconomyService(database, audit, this);
        rtpService = new RtpService(this, messages, teleportManager, audit);
        tablistService = new TablistService(this, messages);
        tablistService.start();
        vanishService = new VanishService(this);
        combatService = new CombatService(this);
        menuService = new StaffMenuService(this, messages, vanishService, socialSpyService, muteChatService,
                playerService, punishmentService);

        PluginWiring wiring = new PluginWiring(this);
        wiring.registerAll();
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.shutdown();
        }
        if (tablistService != null) {
            tablistService.stop();
        }
    }

    public static SmpCorePlugin getInstance() {
        return Objects.requireNonNull(instance, "plugin");
    }

    public MessageService getMessages() {
        return messages;
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public AuditService getAudit() {
        return audit;
    }

    public CooldownManager getCooldowns() {
        return cooldowns;
    }

    public WarmupManager getWarmups() {
        return warmups;
    }

    public BackManager getBackManager() {
        return backManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public TpaService getTpaService() {
        return tpaService;
    }

    public HomeService getHomeService() {
        return homeService;
    }

    public WarpService getWarpService() {
        return warpService;
    }

    public IgnoreService getIgnoreService() {
        return ignoreService;
    }

    public ChatService getChatService() {
        return chatService;
    }

    public SocialSpyService getSocialSpyService() {
        return socialSpyService;
    }

    public MuteChatService getMuteChatService() {
        return muteChatService;
    }

    public PunishmentService getPunishmentService() {
        return punishmentService;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public RtpService getRtpService() {
        return rtpService;
    }

    public PlayerService getPlayerService() {
        return playerService;
    }

    public TablistService getTablistService() {
        return tablistService;
    }

    public VanishService getVanishService() {
        return vanishService;
    }

    public CombatService getCombatService() {
        return combatService;
    }

    public StaffMenuService getMenuService() {
        return menuService;
    }
}

