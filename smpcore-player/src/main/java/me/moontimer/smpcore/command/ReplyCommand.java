package me.moontimer.smpcore.command;

import java.util.Map;
import java.util.StringJoiner;
import me.moontimer.smpcore.chat.ChatService;
import me.moontimer.smpcore.core.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ReplyCommand extends BaseCommand {
    private final ChatService chatService;

    public ReplyCommand(Plugin plugin, MessageService messages, ChatService chatService) {
        super(plugin, messages);
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!checkPermission(sender, "smpcore.reply.use")) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, "/reply <message>");
            return true;
        }
        Player target = chatService.getReplyTarget(player);
        if (target == null) {
            messages.send(player, "msg.reply-none");
            return true;
        }
        StringJoiner joiner = new StringJoiner(" ");
        for (String arg : args) {
            joiner.add(arg);
        }
        boolean sent = chatService.sendPrivateMessage(player, target, joiner.toString());
        if (!sent) {
            messages.send(player, "errors.cannot-message");
        }
        return true;
    }
}

