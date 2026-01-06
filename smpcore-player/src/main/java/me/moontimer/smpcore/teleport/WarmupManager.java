package me.moontimer.smpcore.teleport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class WarmupManager {
    private final Plugin plugin;
    private final Map<UUID, PendingWarmup> pending = new ConcurrentHashMap<>();

    public WarmupManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void startWarmup(Player player, int seconds, boolean cancelOnMove, boolean cancelOnDamage,
                             Runnable onComplete, Runnable onCancel) {
        startWarmup(player, seconds, cancelOnMove, cancelOnDamage, onComplete, onCancel, null);
    }

    public void startWarmup(Player player, int seconds, boolean cancelOnMove, boolean cancelOnDamage,
                             Runnable onComplete, Runnable onCancel, IntConsumer onTick) {
        cancel(player.getUniqueId());
        Location origin = player.getLocation().clone();
        AtomicReference<BukkitTask> tickTaskRef = new AtomicReference<>();
        BukkitTask tickTask = null;
        if (onTick != null && seconds > 0) {
            onTick.accept(seconds);
            tickTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                private int remaining = seconds;

                @Override
                public void run() {
                    remaining--;
                    if (remaining <= 0) {
                        BukkitTask task = tickTaskRef.get();
                        if (task != null) {
                            task.cancel();
                        }
                        return;
                    }
                    onTick.accept(remaining);
                }
            }, 20L, 20L);
            tickTaskRef.set(tickTask);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingWarmup existing = pending.remove(player.getUniqueId());
            if (existing != null && existing.tickTask != null) {
                existing.tickTask.cancel();
            }
            onComplete.run();
        }, seconds * 20L);
        pending.put(player.getUniqueId(), new PendingWarmup(origin, cancelOnMove, cancelOnDamage, task, tickTask, onCancel));
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public PendingWarmup getPending(UUID uuid) {
        return pending.get(uuid);
    }

    public void cancel(UUID uuid) {
        PendingWarmup existing = pending.remove(uuid);
        if (existing != null) {
            existing.task.cancel();
            if (existing.tickTask != null) {
                existing.tickTask.cancel();
            }
            if (existing.onCancel != null) {
                existing.onCancel.run();
            }
        }
    }

    public static class PendingWarmup {
        private final Location origin;
        private final boolean cancelOnMove;
        private final boolean cancelOnDamage;
        private final BukkitTask task;
        private final BukkitTask tickTask;
        private final Runnable onCancel;

        public PendingWarmup(Location origin, boolean cancelOnMove, boolean cancelOnDamage, BukkitTask task,
                             BukkitTask tickTask, Runnable onCancel) {
            this.origin = origin;
            this.cancelOnMove = cancelOnMove;
            this.cancelOnDamage = cancelOnDamage;
            this.task = task;
            this.tickTask = tickTask;
            this.onCancel = onCancel;
        }

        public Location getOrigin() {
            return origin;
        }

        public boolean isCancelOnMove() {
            return cancelOnMove;
        }

        public boolean isCancelOnDamage() {
            return cancelOnDamage;
        }
    }
}

