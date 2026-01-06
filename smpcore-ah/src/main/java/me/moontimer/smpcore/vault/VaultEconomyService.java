package me.moontimer.smpcore.vault;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.Plugin;

public class VaultEconomyService {
    private final Plugin plugin;
    private Economy economy;

    public VaultEconomyService(Plugin plugin) {
        this.plugin = plugin;
        setup();
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return false;
        }
        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
        economy = provider != null ? provider.getProvider() : null;
        return economy != null;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }
}
