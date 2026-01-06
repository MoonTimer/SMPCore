package me.moontimer.smpcore;

import me.moontimer.smpcore.auction.AuctionMenuListener;
import me.moontimer.smpcore.auction.AuctionMenuService;
import me.moontimer.smpcore.auction.AuctionOpenListener;
import me.moontimer.smpcore.auction.AuctionService;
import me.moontimer.smpcore.command.AuctionCommand;
import me.moontimer.smpcore.core.CommandRegistrar;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.vault.VaultEconomyService;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmpCoreAuctionPlugin extends JavaPlugin {
    private SmpCorePlugin core;
    private AuctionService auctionService;

    @Override
    public void onEnable() {
        core = resolveCore();
        if (core == null) {
            return;
        }
        MessageService messages = core.getMessages();

        VaultEconomyService vaultEconomy = new VaultEconomyService(core);
        if (!vaultEconomy.isAvailable()) {
            getLogger().warning("Vault Economy nicht gefunden. Auktionshaus-Transaktionen sind deaktiviert.");
        }
        auctionService = new AuctionService(core, core.getDatabase(), vaultEconomy, core.getAudit());
        AuctionMenuService auctionMenuService = new AuctionMenuService(core, messages, auctionService);
        auctionService.startCleanupTask();

        CommandRegistrar registrar = new CommandRegistrar(this, core, messages);
        registrar.registerListener(new AuctionMenuListener(auctionMenuService));
        registrar.registerListener(new AuctionOpenListener(core, auctionMenuService));
        registrar.registerCommand("ah", new AuctionCommand(core, messages, auctionService, auctionMenuService));
    }

    @Override
    public void onDisable() {
        if (auctionService != null) {
            auctionService.stopCleanupTask();
        }
    }

    private SmpCorePlugin resolveCore() {
        Plugin plugin = getServer().getPluginManager().getPlugin("SMPCore");
        if (plugin instanceof SmpCorePlugin corePlugin && plugin.isEnabled()) {
            return corePlugin;
        }
        getLogger().severe("SMPCore Core plugin not found; disabling SMPCore-AH.");
        getServer().getPluginManager().disablePlugin(this);
        return null;
    }
}
