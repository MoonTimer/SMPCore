package me.moontimer.smpcore.core;

import java.util.List;
import me.moontimer.smpcore.SmpCorePlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandRegistrar {
    private final JavaPlugin plugin;
    private final SmpCorePlugin core;
    private final MessageService messages;

    public CommandRegistrar(JavaPlugin plugin, SmpCorePlugin core, MessageService messages) {
        this.plugin = plugin;
        this.core = core;
        this.messages = messages;
    }

    public void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Command not found in plugin.yml: " + name);
            return;
        }
        GuardedCommandExecutor guarded = new GuardedCommandExecutor(name, executor);
        command.setExecutor(guarded);
        if (executor instanceof TabCompleter) {
            command.setTabCompleter(guarded);
        }
    }

    private boolean isCommandEnabled(String name) {
        FileConfiguration config = core.getConfig();
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
        String raw = messages.getRaw("errors.command-disabled");
        if (raw == null || raw.isEmpty()) {
            String prefix = messages.get("prefix");
            String fallback = messages.colorize("&cBefehl ist deaktiviert.");
            sender.sendMessage(prefix + fallback);
            return;
        }
        messages.send(sender, "errors.command-disabled");
    }

    private final class GuardedCommandExecutor implements CommandExecutor, TabCompleter {
        private final String commandName;
        private final CommandExecutor delegate;
        private final TabCompleter tabCompleter;

        private GuardedCommandExecutor(String commandName, CommandExecutor delegate) {
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
    }
}
