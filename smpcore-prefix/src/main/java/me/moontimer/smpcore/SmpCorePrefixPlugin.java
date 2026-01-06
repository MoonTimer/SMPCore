package me.moontimer.smpcore;

import me.moontimer.smpcore.command.TablistCommand;
import me.moontimer.smpcore.core.CommandRegistrar;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.RankPrefixService;
import me.moontimer.smpcore.core.SmpCoreReloadable;
import me.moontimer.smpcore.core.TablistService;
import me.moontimer.smpcore.listener.TablistJoinListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmpCorePrefixPlugin extends JavaPlugin implements SmpCoreReloadable {
    private SmpCorePlugin core;
    private RankPrefixService rankPrefixService;
    private TablistService tablistService;

    @Override
    public void onEnable() {
        core = resolveCore();
        if (core == null) {
            return;
        }
        MessageService messages = core.getMessages();
        rankPrefixService = new RankPrefixService(core, messages);
        tablistService = new TablistService(core, messages, rankPrefixService);
        tablistService.start();

        CommandRegistrar registrar = new CommandRegistrar(this, core, messages);
        registrar.registerListener(new TablistJoinListener(tablistService));
        registrar.registerCommand("tablist", new TablistCommand(core, messages, tablistService));
    }

    @Override
    public void onDisable() {
        if (tablistService != null) {
            tablistService.stop();
        }
    }

    @Override
    public void reload() {
        if (rankPrefixService != null) {
            rankPrefixService.reload();
        }
        if (tablistService != null) {
            tablistService.reload();
        }
    }

    private SmpCorePlugin resolveCore() {
        Plugin plugin = getServer().getPluginManager().getPlugin("SMPCore");
        if (plugin instanceof SmpCorePlugin corePlugin && plugin.isEnabled()) {
            return corePlugin;
        }
        getLogger().severe("SMPCore Core plugin not found; disabling SMPCore-Prefix.");
        getServer().getPluginManager().disablePlugin(this);
        return null;
    }

    public RankPrefixService getRankPrefixService() {
        return rankPrefixService;
    }

    public TablistService getTablistService() {
        return tablistService;
    }
}
