package me.moontimer.smpcore.rtp;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import me.moontimer.smpcore.audit.AuditService;
import me.moontimer.smpcore.core.MessageService;
import me.moontimer.smpcore.teleport.TeleportCause;
import me.moontimer.smpcore.teleport.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RtpService {
    private final Plugin plugin;
    private final MessageService messages;
    private final TeleportManager teleportManager;
    private final AuditService audit;
    private final Random random = new Random();

    public RtpService(Plugin plugin, MessageService messages, TeleportManager teleportManager, AuditService audit) {
        this.plugin = plugin;
        this.messages = messages;
        this.teleportManager = teleportManager;
        this.audit = audit;
    }

    public void requestRtp(Player player, World world) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("rtp.enabled", true)) {
            messages.send(player, "errors.rtp-disabled");
            return;
        }
        if (world == null) {
            messages.send(player, "errors.rtp-failed");
            return;
        }
        messages.send(player, "rtp.searching");
        int maxTries = config.getInt("rtp.max-tries", 20);
        Set<String> blockedBiomes = new HashSet<>(config.getStringList("rtp.blacklist-biomes"));
        boolean requireSolid = config.getBoolean("rtp.safe-check.require-solid-below", true);
        boolean denyWater = config.getBoolean("rtp.safe-check.deny-water", true);
        boolean denyLava = config.getBoolean("rtp.safe-check.deny-lava", true);
        boolean denyLeaves = config.getBoolean("rtp.safe-check.deny-leaves", false);
        CoordinateRange range = getWorldRange(world);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location found = findRandomLocation(world, range, maxTries, blockedBiomes,
                    requireSolid, denyWater, denyLava, denyLeaves);
            if (found == null) {
                int fallbackTries = Math.max(100, maxTries * 5);
                found = findRandomLocation(world, range, fallbackTries, Set.of(),
                        false, false, denyLava, false);
            }
            if (found == null) {
                found = findFallbackNearSpawn(world, denyLava);
            }
            Location finalLocation = found == null ? world.getSpawnLocation() : found;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalLocation == null) {
                    messages.send(player, "errors.rtp-failed");
                    return;
                }
                TeleportManager.TeleportRequest request = new TeleportManager.TeleportRequest(
                        "rtp",
                        config.getInt("rtp.cooldown-seconds", 300),
                        config.getInt("rtp.warmup-seconds", 5),
                        true,
                        null,
                        "smpcore.teleport.bypass.warmup",
                        "smpcore.rtp.cooldown.bypass",
                        () -> messages.send(player, "rtp.teleport")
                );
                teleportManager.requestTeleport(player, finalLocation, TeleportCause.RTP, request);
                audit.log(player.getUniqueId(), "rtp", Map.of("world", world.getName()));
            });
        });
    }

    private CoordinateRange getWorldRange(World world) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double radius = border.getSize() / 2.0;
        if (radius <= 1.0) {
            int x = center.getBlockX();
            int z = center.getBlockZ();
            return new CoordinateRange(x, x, z, z);
        }
        double safeRadius = Math.max(0.0, radius - 1.0);
        int minX = (int) Math.floor(center.getX() - safeRadius);
        int maxX = (int) Math.ceil(center.getX() + safeRadius);
        int minZ = (int) Math.floor(center.getZ() - safeRadius);
        int maxZ = (int) Math.ceil(center.getZ() + safeRadius);
        if (minX > maxX) {
            int tmp = minX;
            minX = maxX;
            maxX = tmp;
        }
        if (minZ > maxZ) {
            int tmp = minZ;
            minZ = maxZ;
            maxZ = tmp;
        }
        return new CoordinateRange(minX, maxX, minZ, maxZ);
    }

    private Location findRandomLocation(World world, CoordinateRange range, int tries, Set<String> blockedBiomes,
                                        boolean requireSolid, boolean denyWater, boolean denyLava, boolean denyLeaves) {
        for (int i = 0; i < tries; i++) {
            int x = range.randomX(random);
            int z = range.randomZ(random);
            Location candidate = findSafeLocation(world, x, z, blockedBiomes, requireSolid, denyWater, denyLava, denyLeaves);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private Location findFallbackNearSpawn(World world, boolean denyLava) {
        Location spawn = world.getSpawnLocation();
        int baseX = spawn.getBlockX();
        int baseZ = spawn.getBlockZ();
        int radius = 64;
        Set<String> noBiomes = Set.of();
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    Location candidate = findSafeLocation(world, baseX + dx, baseZ + dz, noBiomes,
                            false, false, denyLava, false);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private Location findSafeLocation(World world, int x, int z, Set<String> blockedBiomes,
                                      boolean requireSolid, boolean denyWater, boolean denyLava, boolean denyLeaves) {
        try {
            return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    if (!world.loadChunk(chunkX, chunkZ, true)) {
                        return null;
                    }
                }
                int minY = world.getMinHeight();
                int maxY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                int maxWorldY = world.getMaxHeight() - 1;
                if (maxY > maxWorldY) {
                    maxY = maxWorldY;
                }
                for (int y = maxY; y > minY; y--) {
                    if (y + 1 >= world.getMaxHeight()) {
                        continue;
                    }
                    Block ground = world.getBlockAt(x, y - 1, z);
                    if (requireSolid && !ground.getType().isSolid()) {
                        continue;
                    }
                    Material groundType = ground.getType();
                    if (denyWater && isWater(groundType)) {
                        continue;
                    }
                    if (denyLava && (groundType == Material.LAVA)) {
                        continue;
                    }
                    if (denyLeaves && groundType.name().endsWith("LEAVES")) {
                        continue;
                    }
                    Block feet = world.getBlockAt(x, y, z);
                    Block head = world.getBlockAt(x, y + 1, z);
                    if (!feet.isPassable() || !head.isPassable()) {
                        continue;
                    }
                    if (denyWater && (isWater(feet.getType()) || isWater(head.getType()))) {
                        continue;
                    }
                    if (denyLava && (feet.getType() == Material.LAVA || head.getType() == Material.LAVA)) {
                        continue;
                    }
                    String biome = world.getBiome(x, y, z).toString();
                    if (blockedBiomes.contains(biome)) {
                        continue;
                    }
                    return new Location(world, x + 0.5, y, z + 0.5);
                }
                return null;
            }).get();
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isWater(Material material) {
        return material == Material.WATER
                || material == Material.KELP
                || material == Material.KELP_PLANT
                || material == Material.SEAGRASS
                || material == Material.TALL_SEAGRASS
                || material == Material.BUBBLE_COLUMN;
    }

    private record CoordinateRange(int minX, int maxX, int minZ, int maxZ) {
        int randomX(Random random) {
            return randomBetween(random, minX, maxX);
        }

        int randomZ(Random random) {
            return randomBetween(random, minZ, maxZ);
        }

        private int randomBetween(Random random, int min, int max) {
            if (max <= min) {
                return min;
            }
            return min + random.nextInt(max - min + 1);
        }
    }
}

