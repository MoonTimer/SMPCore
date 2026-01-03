package me.moontimer.smpcore;

import me.moontimer.smpcore.command.*;
import me.moontimer.smpcore.listener.BackListener;
import me.moontimer.smpcore.listener.ChatListener;
import me.moontimer.smpcore.listener.CombatListener;
import me.moontimer.smpcore.listener.MotdListener;
import me.moontimer.smpcore.listener.PlayerConnectionListener;
import me.moontimer.smpcore.listener.PreLoginListener;
import me.moontimer.smpcore.listener.WarmupListener;
import me.moontimer.smpcore.menu.MenuListener;
import me.moontimer.smpcore.core.MessageService;
import java.util.List;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

public class PluginWiring {
    private final SmpCorePlugin plugin;

    public PluginWiring(SmpCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        registerListeners();
        registerCommands();
    }

    private void registerListeners() {
        registerListener(new PlayerConnectionListener(
                plugin.getPlayerService(),
                plugin.getIgnoreService(),
                plugin.getPunishmentService(),
                plugin.getTablistService(),
                plugin.getVanishService()
        ));
        registerListener(new BackListener(plugin.getBackManager(), plugin));
        registerListener(new WarmupListener(plugin.getWarmups()));
        registerListener(new ChatListener(
                plugin.getPunishmentService(),
                plugin.getMuteChatService(),
                plugin.getMessages()
        ));
        registerListener(new PreLoginListener(plugin.getPunishmentService()));
        registerListener(new MotdListener(plugin, plugin.getMessages()));
        registerListener(new CombatListener(plugin.getCombatService(), plugin.getMessages()));
        registerListener(new MenuListener(plugin.getMenuService()));
    }

    private void registerCommands() {
        registerCommand("spawn", new SpawnCommand(plugin, plugin.getMessages(), plugin.getTeleportManager()));
        registerCommand("sethome", new SetHomeCommand(plugin, plugin.getMessages(), plugin.getHomeService()));
        registerCommand("home", new HomeCommand(plugin, plugin.getMessages(), plugin.getHomeService(), plugin.getTeleportManager()));
        registerCommand("delhome", new DelHomeCommand(plugin, plugin.getMessages(), plugin.getHomeService()));
        registerCommand("homes", new HomesCommand(plugin, plugin.getMessages(), plugin.getHomeService()));
        registerCommand("back", new BackCommand(plugin, plugin.getMessages(), plugin.getBackManager(), plugin.getTeleportManager()));

        registerCommand("tpa", new TpaCommand(plugin, plugin.getMessages(), plugin.getTpaService(), plugin.getCooldowns(), plugin.getCombatService()));
        registerCommand("tpahere", new TpaHereCommand(plugin, plugin.getMessages(), plugin.getTpaService(), plugin.getCooldowns(), plugin.getCombatService()));
        registerCommand("tpaccept", new TpAcceptCommand(plugin, plugin.getMessages(), plugin.getTpaService(), plugin.getTeleportManager(), plugin.getCombatService()));
        registerCommand("tpdeny", new TpDenyCommand(plugin, plugin.getMessages(), plugin.getTpaService()));
        registerCommand("tpacancel", new TpaCancelCommand(plugin, plugin.getMessages(), plugin.getTpaService()));

        registerCommand("warp", new WarpCommand(plugin, plugin.getMessages(), plugin.getWarpService(), plugin.getTeleportManager()));
        registerCommand("setwarp", new SetWarpCommand(plugin, plugin.getMessages(), plugin.getWarpService()));
        registerCommand("delwarp", new DelWarpCommand(plugin, plugin.getMessages(), plugin.getWarpService()));
        registerCommand("warps", new WarpsCommand(plugin, plugin.getMessages(), plugin.getWarpService()));

        registerCommand("rtp", new RtpCommand(plugin, plugin.getMessages(), plugin.getRtpService(), plugin.getCombatService()));

        registerCommand("msg", new MsgCommand(plugin, plugin.getMessages(), plugin.getChatService(), plugin.getCooldowns()));
        registerCommand("reply", new ReplyCommand(plugin, plugin.getMessages(), plugin.getChatService()));
        registerCommand("ignore", new IgnoreCommand(plugin, plugin.getMessages(), plugin.getIgnoreService()));
        registerCommand("ignorelist", new IgnoreListCommand(plugin, plugin.getMessages(), plugin.getIgnoreService()));
        registerCommand("socialspy", new SocialSpyCommand(plugin, plugin.getMessages(), plugin.getSocialSpyService()));
        registerCommand("mutechat", new MuteChatCommand(plugin, plugin.getMessages(), plugin.getMuteChatService()));

        registerCommand("gamemode", new GamemodeCommand(plugin, plugin.getMessages()));
        registerCommand("fly", new FlyCommand(plugin, plugin.getMessages()));
        registerCommand("tp", new TpCommand(plugin, plugin.getMessages()));
        registerCommand("tphere", new TpHereCommand(plugin, plugin.getMessages()));
        registerCommand("whois", new WhoisCommand(plugin, plugin.getMessages(), plugin.getPlayerService()));
        registerCommand("info", new InfoCommand(plugin, plugin.getMessages(), plugin.getPlayerService(),
                plugin.getPunishmentService(), plugin.getMenuService()));
        registerCommand("heal", new HealCommand(plugin, plugin.getMessages()));
        registerCommand("feed", new FeedCommand(plugin, plugin.getMessages()));
        registerCommand("speed", new SpeedCommand(plugin, plugin.getMessages()));
        registerCommand("invsee", new InvseeCommand(plugin, plugin.getMessages()));
        registerCommand("endersee", new EnderseeCommand(plugin, plugin.getMessages()));
        registerCommand("vanish", new VanishCommand(plugin, plugin.getMessages(), plugin.getVanishService()));
        registerCommand("vtp", new VtpCommand(plugin, plugin.getMessages(), plugin.getVanishService()));
        registerCommand("tpall", new TpAllCommand(plugin, plugin.getMessages()));
        registerCommand("tppos", new TpPosCommand(plugin, plugin.getMessages()));
        registerCommand("clearchat", new ClearChatCommand(plugin, plugin.getMessages()));

        registerCommand("ban", new BanCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("tempban", new TempBanCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("unban", new UnbanCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("mute", new MuteCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("tempmute", new TempMuteCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("unmute", new UnmuteCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("warn", new WarnCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("unwarn", new UnwarnCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("clearwarnings", new ClearWarningsCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("warnings", new WarningsCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("kick", new KickCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("reasons", new ReasonsCommand(plugin, plugin.getMessages()));
        registerCommand("history", new HistoryCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("staffhistory", new StaffHistoryCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));
        registerCommand("banlist", new BanListCommand(plugin, plugin.getMessages(), plugin.getPunishmentService()));

        registerCommand("balance", new BalanceCommand(plugin, plugin.getMessages(), plugin.getEconomyService()));
        registerCommand("pay", new PayCommand(plugin, plugin.getMessages(), plugin.getEconomyService()));

        registerCommand("help", new HelpCommand(plugin, plugin.getMessages()));
        registerCommand("menu", new MenuCommand(plugin, plugin.getMessages(), plugin.getMenuService()));
        registerCommand("tablist", new TablistCommand(plugin, plugin.getMessages(), plugin.getTablistService()));
        registerCommand("motd", new MotdCommand(plugin, plugin.getMessages()));
        registerCommand("smpcore", new SmpCoreCommand(plugin, plugin.getMessages()));
    }

    private void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Command not found in plugin.yml: " + name);
            return;
        }
        GuardedCommandExecutor guarded = new GuardedCommandExecutor(plugin, name, executor);
        command.setExecutor(guarded);
        if (executor instanceof TabCompleter) {
            command.setTabCompleter(guarded);
        }
    }

    private static final class GuardedCommandExecutor implements CommandExecutor, TabCompleter {
        private final SmpCorePlugin plugin;
        private final String commandName;
        private final CommandExecutor delegate;
        private final TabCompleter tabCompleter;

        private GuardedCommandExecutor(SmpCorePlugin plugin, String commandName, CommandExecutor delegate) {
            this.plugin = plugin;
            this.commandName = commandName;
            this.delegate = delegate;
            this.tabCompleter = delegate instanceof TabCompleter completer ? completer : null;
        }

        @Override
        public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
            if (!isCommandEnabled(commandName)) {
                sendDisabled(sender);
                return true;
            }
            return delegate.onCommand(sender, command, label, args);
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias,
                                           String[] args) {
            if (!isCommandEnabled(commandName)) {
                return List.of();
            }
            if (tabCompleter == null) {
                return List.of();
            }
            return tabCompleter.onTabComplete(sender, command, alias, args);
        }

        private boolean isCommandEnabled(String name) {
            FileConfiguration config = plugin.getConfig();
            ConfigurationSection features = config.getConfigurationSection("features");
            if (features == null) {
                return true;
            }
            for (String category : features.getKeys(false)) {
                ConfigurationSection categorySection = features.getConfigurationSection(category);
                if (categorySection == null) {
                    continue;
                }
                boolean categoryEnabled = categorySection.getBoolean("enabled", true);
                ConfigurationSection commands = categorySection.getConfigurationSection("commands");
                if (commands == null || !commands.contains(name)) {
                    continue;
                }
                boolean commandEnabled = commands.getBoolean(name, true);
                return categoryEnabled && commandEnabled;
            }
            return true;
        }

        private void sendDisabled(CommandSender sender) {
            MessageService messages = plugin.getMessages();
            String raw = messages.getRaw("errors.command-disabled");
            if (raw == null || raw.isEmpty()) {
                String prefix = messages.get("prefix");
                String fallback = messages.colorize("&cBefehl ist deaktiviert.");
                sender.sendMessage(prefix + fallback);
                return;
            }
            messages.send(sender, "errors.command-disabled");
        }
    }
}

