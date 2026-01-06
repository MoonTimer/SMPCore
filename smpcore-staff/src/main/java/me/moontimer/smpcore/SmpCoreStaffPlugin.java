package me.moontimer.smpcore;

import me.moontimer.smpcore.command.BanCommand;
import me.moontimer.smpcore.command.BanListCommand;
import me.moontimer.smpcore.command.ClearChatCommand;
import me.moontimer.smpcore.command.ClearWarningsCommand;
import me.moontimer.smpcore.command.EnderseeCommand;
import me.moontimer.smpcore.command.FeedCommand;
import me.moontimer.smpcore.command.FlyCommand;
import me.moontimer.smpcore.command.GamemodeCommand;
import me.moontimer.smpcore.command.HealCommand;
import me.moontimer.smpcore.command.HistoryCommand;
import me.moontimer.smpcore.command.InfoCommand;
import me.moontimer.smpcore.command.InvseeCommand;
import me.moontimer.smpcore.command.KickCommand;
import me.moontimer.smpcore.command.MenuCommand;
import me.moontimer.smpcore.command.MotdCommand;
import me.moontimer.smpcore.command.MuteChatCommand;
import me.moontimer.smpcore.command.MuteCommand;
import me.moontimer.smpcore.command.ReasonsCommand;
import me.moontimer.smpcore.command.SetSpawnCommand;
import me.moontimer.smpcore.command.SocialSpyCommand;
import me.moontimer.smpcore.command.SpeedCommand;
import me.moontimer.smpcore.command.StaffHistoryCommand;
import me.moontimer.smpcore.command.TempBanCommand;
import me.moontimer.smpcore.command.TempMuteCommand;
import me.moontimer.smpcore.command.TpAllCommand;
import me.moontimer.smpcore.command.TpCommand;
import me.moontimer.smpcore.command.TpHereCommand;
import me.moontimer.smpcore.command.TpPosCommand;
import me.moontimer.smpcore.command.UnbanCommand;
import me.moontimer.smpcore.command.UnmuteCommand;
import me.moontimer.smpcore.command.UnwarnCommand;
import me.moontimer.smpcore.command.VanishCommand;
import me.moontimer.smpcore.command.VtpCommand;
import me.moontimer.smpcore.command.WarnCommand;
import me.moontimer.smpcore.command.WarningsCommand;
import me.moontimer.smpcore.command.WhoisCommand;
import me.moontimer.smpcore.core.CommandRegistrar;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.PlayerService;
import me.moontimer.smpcore.core.RankPrefixService;
import me.moontimer.smpcore.listener.ChatListener;
import me.moontimer.smpcore.listener.MotdListener;
import me.moontimer.smpcore.listener.PreLoginListener;
import me.moontimer.smpcore.listener.StaffConnectionListener;
import me.moontimer.smpcore.menu.MenuListener;
import me.moontimer.smpcore.menu.StaffMenuService;
import me.moontimer.smpcore.moderation.PunishmentService;
import me.moontimer.smpcore.staff.VanishService;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmpCoreStaffPlugin extends JavaPlugin {
    private SmpCorePlugin core;
    private SmpCorePrefixPlugin prefix;

    private PunishmentService punishmentService;
    private VanishService vanishService;
    private StaffMenuService menuService;

    @Override
    public void onEnable() {
        core = resolveCore();
        if (core == null) {
            return;
        }
        prefix = resolvePrefix();
        if (prefix == null) {
            return;
        }
        MessageService messages = core.getMessages();
        PlayerService playerService = core.getPlayerService();
        RankPrefixService rankPrefixService = prefix.getRankPrefixService();

        punishmentService = new PunishmentService(core, core.getDatabase(), core.getAudit(), messages);
        vanishService = new VanishService(core);
        menuService = new StaffMenuService(core, messages, vanishService, core.getSocialSpyService(),
                core.getMuteChatService(), playerService, punishmentService);

        CommandRegistrar registrar = new CommandRegistrar(this, core, messages);
        registrar.registerListener(new PreLoginListener(punishmentService));
        registrar.registerListener(new ChatListener(punishmentService, core.getMuteChatService(), messages,
                rankPrefixService));
        registrar.registerListener(new MotdListener(core, messages));
        registrar.registerListener(new StaffConnectionListener(punishmentService, vanishService));
        registrar.registerListener(new MenuListener(menuService));

        registrar.registerCommand("setspawn", new SetSpawnCommand(core, messages));
        registrar.registerCommand("gamemode", new GamemodeCommand(core, messages));
        registrar.registerCommand("fly", new FlyCommand(core, messages));
        registrar.registerCommand("tp", new TpCommand(core, messages));
        registrar.registerCommand("tphere", new TpHereCommand(core, messages));
        registrar.registerCommand("whois", new WhoisCommand(core, messages, playerService));
        registrar.registerCommand("info", new InfoCommand(core, messages, playerService, punishmentService, menuService));
        registrar.registerCommand("heal", new HealCommand(core, messages));
        registrar.registerCommand("feed", new FeedCommand(core, messages));
        registrar.registerCommand("speed", new SpeedCommand(core, messages));
        registrar.registerCommand("invsee", new InvseeCommand(core, messages));
        registrar.registerCommand("endersee", new EnderseeCommand(core, messages));
        registrar.registerCommand("vanish", new VanishCommand(core, messages, vanishService));
        registrar.registerCommand("vtp", new VtpCommand(core, messages, vanishService));
        registrar.registerCommand("tpall", new TpAllCommand(core, messages));
        registrar.registerCommand("tppos", new TpPosCommand(core, messages));
        registrar.registerCommand("clearchat", new ClearChatCommand(core, messages));
        registrar.registerCommand("socialspy", new SocialSpyCommand(core, messages, core.getSocialSpyService()));
        registrar.registerCommand("mutechat", new MuteChatCommand(core, messages, core.getMuteChatService()));

        registrar.registerCommand("ban", new BanCommand(core, messages, punishmentService));
        registrar.registerCommand("tempban", new TempBanCommand(core, messages, punishmentService));
        registrar.registerCommand("unban", new UnbanCommand(core, messages, punishmentService));
        registrar.registerCommand("mute", new MuteCommand(core, messages, punishmentService));
        registrar.registerCommand("tempmute", new TempMuteCommand(core, messages, punishmentService));
        registrar.registerCommand("unmute", new UnmuteCommand(core, messages, punishmentService));
        registrar.registerCommand("warn", new WarnCommand(core, messages, punishmentService));
        registrar.registerCommand("unwarn", new UnwarnCommand(core, messages, punishmentService));
        registrar.registerCommand("clearwarnings", new ClearWarningsCommand(core, messages, punishmentService));
        registrar.registerCommand("warnings", new WarningsCommand(core, messages, punishmentService));
        registrar.registerCommand("kick", new KickCommand(core, messages, punishmentService));
        registrar.registerCommand("reasons", new ReasonsCommand(core, messages));
        registrar.registerCommand("history", new HistoryCommand(core, messages, punishmentService));
        registrar.registerCommand("staffhistory", new StaffHistoryCommand(core, messages, punishmentService));
        registrar.registerCommand("banlist", new BanListCommand(core, messages, punishmentService));

        registrar.registerCommand("menu", new MenuCommand(core, messages, menuService));
        registrar.registerCommand("motd", new MotdCommand(core, messages));
    }

    private SmpCorePlugin resolveCore() {
        Plugin plugin = getServer().getPluginManager().getPlugin("SMPCore");
        if (plugin instanceof SmpCorePlugin corePlugin && plugin.isEnabled()) {
            return corePlugin;
        }
        getLogger().severe("SMPCore Core plugin not found; disabling SMPCore-Staff.");
        getServer().getPluginManager().disablePlugin(this);
        return null;
    }

    private SmpCorePrefixPlugin resolvePrefix() {
        Plugin plugin = getServer().getPluginManager().getPlugin("SMPCore-Prefix");
        if (plugin instanceof SmpCorePrefixPlugin prefixPlugin && plugin.isEnabled()) {
            return prefixPlugin;
        }
        getLogger().severe("SMPCore-Prefix plugin not found; disabling SMPCore-Staff.");
        getServer().getPluginManager().disablePlugin(this);
        return null;
    }
}
