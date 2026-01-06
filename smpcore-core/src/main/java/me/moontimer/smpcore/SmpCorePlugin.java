package me.moontimer.smpcore;

import java.util.Objects;
import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.chat.MuteChatService;
import me.moontimer.smpcore.chat.SocialSpyService;
import me.moontimer.smpcore.command.SmpCoreCommand;
import me.moontimer.smpcore.core.CommandRegistrar;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.core.PlayerService;
import me.moontimer.smpcore.db.DatabaseManager;
import me.moontimer.smpcore.listener.CommandSecurityListener;
import me.moontimer.smpcore.listener.CorePlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmpCorePlugin extends JavaPlugin {
    private static SmpCorePlugin instance;

    private MessageService messages;
    private DatabaseManager database;
    private AuditService audit;
    private PlayerService playerService;
    private SocialSpyService socialSpyService;
    private MuteChatService muteChatService;

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
        playerService = new PlayerService(this, database);
        socialSpyService = new SocialSpyService();
        muteChatService = new MuteChatService();

        CommandRegistrar registrar = new CommandRegistrar(this, this, messages);
        registrar.registerListener(new CommandSecurityListener(messages));
        registrar.registerListener(new CorePlayerListener(playerService));
        registrar.registerCommand("smpcore", new SmpCoreCommand(this, messages));
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.shutdown();
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

    public SocialSpyService getSocialSpyService() {
        return socialSpyService;
    }

    public MuteChatService getMuteChatService() {
        return muteChatService;
    }

    public PlayerService getPlayerService() {
        return playerService;
    }
}

