package me.moontimer.smpcore;

import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.chat.ChatService;
import me.moontimer.smpcore.chat.IgnoreService;
import me.moontimer.smpcore.combat.CombatService;
import me.moontimer.smpcore.command.BackCommand;
import me.moontimer.smpcore.command.DelHomeCommand;
import me.moontimer.smpcore.command.DelWarpCommand;
import me.moontimer.smpcore.command.HelpCommand;
import me.moontimer.smpcore.command.HomeCommand;
import me.moontimer.smpcore.command.HomesCommand;
import me.moontimer.smpcore.command.IgnoreCommand;
import me.moontimer.smpcore.command.IgnoreListCommand;
import me.moontimer.smpcore.command.MsgCommand;
import me.moontimer.smpcore.command.ReplyCommand;
import me.moontimer.smpcore.command.RtpCommand;
import me.moontimer.smpcore.command.SetHomeCommand;
import me.moontimer.smpcore.command.SetWarpCommand;
import me.moontimer.smpcore.command.SpawnCommand;
import me.moontimer.smpcore.command.TpAcceptCommand;
import me.moontimer.smpcore.command.TpDenyCommand;
import me.moontimer.smpcore.command.TpaCancelCommand;
import me.moontimer.smpcore.command.TpaCommand;
import me.moontimer.smpcore.command.TpaHereCommand;
import me.moontimer.smpcore.command.WarpCommand;
import me.moontimer.smpcore.command.WarpsCommand;
import me.moontimer.smpcore.core.CommandRegistrar;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.listener.BackListener;
import me.moontimer.smpcore.listener.CombatListener;
import me.moontimer.smpcore.listener.IgnoreConnectionListener;
import me.moontimer.smpcore.listener.WarmupListener;
import me.moontimer.smpcore.rtp.RtpService;
import me.moontimer.smpcore.teleport.BackManager;
import me.moontimer.smpcore.teleport.CooldownManager;
import me.moontimer.smpcore.teleport.HomeService;
import me.moontimer.smpcore.teleport.TeleportManager;
import me.moontimer.smpcore.teleport.TpaService;
import me.moontimer.smpcore.teleport.WarmupManager;
import me.moontimer.smpcore.teleport.WarpService;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmpCorePlayerPlugin extends JavaPlugin {
    private SmpCorePlugin core;

    private CooldownManager cooldowns;
    private WarmupManager warmups;
    private BackManager backManager;
    private TeleportManager teleportManager;
    private TpaService tpaService;
    private HomeService homeService;
    private WarpService warpService;
    private IgnoreService ignoreService;
    private ChatService chatService;
    private CombatService combatService;
    private RtpService rtpService;

    @Override
    public void onEnable() {
        core = resolveCore();
        if (core == null) {
            return;
        }
        MessageService messages = core.getMessages();
        AuditService audit = core.getAudit();

        cooldowns = new CooldownManager();
        warmups = new WarmupManager(core);
        backManager = new BackManager();
        teleportManager = new TeleportManager(core, messages, cooldowns, warmups, backManager, audit);
        tpaService = new TpaService(core, messages, teleportManager, audit);
        homeService = new HomeService(core.getDatabase(), audit, core);
        warpService = new WarpService(core.getDatabase(), audit, core);

        ignoreService = new IgnoreService(core.getDatabase(), core);
        chatService = new ChatService(messages, ignoreService, core.getSocialSpyService(), core.getMuteChatService());
        combatService = new CombatService(core);
        rtpService = new RtpService(core, messages, teleportManager, audit);

        CommandRegistrar registrar = new CommandRegistrar(this, core, messages);
        registrar.registerListener(new BackListener(backManager, core));
        registrar.registerListener(new WarmupListener(warmups));
        registrar.registerListener(new CombatListener(combatService, messages));
        registrar.registerListener(new IgnoreConnectionListener(ignoreService));

        registrar.registerCommand("spawn", new SpawnCommand(core, messages, teleportManager));
        registrar.registerCommand("sethome", new SetHomeCommand(core, messages, homeService));
        registrar.registerCommand("home", new HomeCommand(core, messages, homeService, teleportManager));
        registrar.registerCommand("delhome", new DelHomeCommand(core, messages, homeService));
        registrar.registerCommand("homes", new HomesCommand(core, messages, homeService));
        registrar.registerCommand("back", new BackCommand(core, messages, backManager, teleportManager));

        registrar.registerCommand("tpa", new TpaCommand(core, messages, tpaService, cooldowns, combatService));
        registrar.registerCommand("tpahere", new TpaHereCommand(core, messages, tpaService, cooldowns, combatService));
        registrar.registerCommand("tpaccept", new TpAcceptCommand(core, messages, tpaService, teleportManager, combatService));
        registrar.registerCommand("tpdeny", new TpDenyCommand(core, messages, tpaService));
        registrar.registerCommand("tpacancel", new TpaCancelCommand(core, messages, tpaService));

        registrar.registerCommand("warp", new WarpCommand(core, messages, warpService, teleportManager));
        registrar.registerCommand("setwarp", new SetWarpCommand(core, messages, warpService));
        registrar.registerCommand("delwarp", new DelWarpCommand(core, messages, warpService));
        registrar.registerCommand("warps", new WarpsCommand(core, messages, warpService));

        registrar.registerCommand("msg", new MsgCommand(core, messages, chatService, cooldowns));
        registrar.registerCommand("reply", new ReplyCommand(core, messages, chatService));
        registrar.registerCommand("ignore", new IgnoreCommand(core, messages, ignoreService));
        registrar.registerCommand("ignorelist", new IgnoreListCommand(core, messages, ignoreService));

        registrar.registerCommand("help", new HelpCommand(core, messages));
        registrar.registerCommand("rtp", new RtpCommand(core, messages, rtpService, combatService));
    }

    private SmpCorePlugin resolveCore() {
        Plugin plugin = getServer().getPluginManager().getPlugin("SMPCore");
        if (plugin instanceof SmpCorePlugin corePlugin && plugin.isEnabled()) {
            return corePlugin;
        }
        getLogger().severe("SMPCore Core plugin not found; disabling SMPCore-Player.");
        getServer().getPluginManager().disablePlugin(this);
        return null;
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

    public CooldownManager getCooldowns() {
        return cooldowns;
    }

    public CombatService getCombatService() {
        return combatService;
    }

    public RtpService getRtpService() {
        return rtpService;
    }
}
