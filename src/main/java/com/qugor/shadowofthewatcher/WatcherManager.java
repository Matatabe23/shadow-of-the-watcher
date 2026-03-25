package com.qugor.shadowofthewatcher;

import com.qugor.shadowofthewatcher.entity.WatcherEntity;
import com.qugor.shadowofthewatcher.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class WatcherManager {
    private static UUID watcherUuid;
    @Nullable
    private static UUID currentTargetUuid;
    private static long nextSwitchGameTime = -1L;
    private static int fleeCooldownTicks;

    private WatcherManager() {}

    public static void tickOverworld(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            clearWatcher(level);
            currentTargetUuid = null;
            nextSwitchGameTime = -1L;
            fleeCooldownTicks = 0;
            return;
        }
        long switchTicks = Config.watcherTargetSwitchMinutes * 60L * 20L;
        long now = level.getGameTime();
        ensureTarget(level, players, switchTicks, now);
        ServerPlayer target = findPlayer(players, currentTargetUuid);
        if (target == null) {
            return;
        }
        WatcherEntity watcher = findWatcher(level, watcherUuid);
        if (watcher == null || !watcher.isAlive()) {
            spawnWatcher(level, target);
            return;
        }
        watcher.setOwnerUUID(target.getUUID());
        double dx = target.getX() - watcher.getX();
        double dz = target.getZ() - watcher.getZ();
        double distH = Math.sqrt(dx * dx + dz * dz);
        double stalkBlocks = watcher.getRingRadiusBlocks();
        if (stalkBlocks <= 0.0D) {
            stalkBlocks = Config.watcherStalkDistanceChunks * (double) WatcherConstants.BLOCKS_PER_CHUNK;
        }
        double vanishBlocks = Config.watcherVanishWhenCloserThanChunks * (double) WatcherConstants.BLOCKS_PER_CHUNK;
        double pursuitMargin = Config.watcherPursuitMarginBlocks;

        if (distH < vanishBlocks) {
            teleportFarAway(level, target, watcher);
            fleeCooldownTicks = Config.watcherFleeCooldownTicks;
            facePlayer(watcher, target);
            return;
        }

        if (fleeCooldownTicks > 0) {
            fleeCooldownTicks--;
            facePlayer(watcher, target);
            return;
        }

        if (distH > stalkBlocks + pursuitMargin) {
            placeAtRing(target, watcher);
            facePlayer(watcher, target);
            return;
        }

        facePlayer(watcher, target);
    }

    private static void ensureTarget(ServerLevel level, List<ServerPlayer> players, long switchTicks, long now) {
        ServerPlayer current = findPlayer(players, currentTargetUuid);
        if (current == null) {
            pickRandomTarget(level, players);
            nextSwitchGameTime = now + switchTicks;
            repositionWatcherForNewTarget(level, players);
            return;
        }
        if (nextSwitchGameTime < 0L) {
            nextSwitchGameTime = now + switchTicks;
            return;
        }
        if (now < nextSwitchGameTime) {
            return;
        }
        if (players.size() <= 1) {
            nextSwitchGameTime = now + switchTicks;
            return;
        }
        pickRandomDifferentTarget(level, players, current);
        nextSwitchGameTime = now + switchTicks;
        repositionWatcherForNewTarget(level, players);
    }

    private static void repositionWatcherForNewTarget(ServerLevel level, List<ServerPlayer> players) {
        WatcherEntity watcher = findWatcher(level, watcherUuid);
        if (watcher == null || !watcher.isAlive()) {
            return;
        }
        ServerPlayer target = findPlayer(players, currentTargetUuid);
        if (target == null) {
            return;
        }
        placeAtRing(target, watcher);
        facePlayer(watcher, target);
    }

    private static void pickRandomTarget(ServerLevel level, List<ServerPlayer> players) {
        if (players.isEmpty()) {
            return;
        }
        currentTargetUuid = players.get(level.random.nextInt(players.size())).getUUID();
    }

    private static void pickRandomDifferentTarget(ServerLevel level, List<ServerPlayer> players, ServerPlayer current) {
        RandomSource random = level.random;
        if (players.size() <= 1) {
            currentTargetUuid = current.getUUID();
            return;
        }
        ServerPlayer next;
        int guard = 0;
        do {
            next = players.get(random.nextInt(players.size()));
            guard++;
        } while (next.getUUID().equals(current.getUUID()) && guard < 64);
        currentTargetUuid = next.getUUID();
    }

    @Nullable
    private static ServerPlayer findPlayer(List<ServerPlayer> players, @Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (ServerPlayer player : players) {
            if (player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    private static void clearWatcher(ServerLevel level) {
        if (watcherUuid == null) {
            return;
        }
        Entity entity = level.getEntity(watcherUuid);
        if (entity != null) {
            entity.discard();
        }
        watcherUuid = null;
    }

    private static WatcherEntity findWatcher(ServerLevel level, UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = level.getEntity(id);
        return entity instanceof WatcherEntity w ? w : null;
    }

    private static void spawnWatcher(ServerLevel level, ServerPlayer target) {
        WatcherEntity watcher = ModEntityTypes.WATCHER.get().create(level);
        if (watcher == null) {
            return;
        }
        watcher.setOwnerUUID(target.getUUID());
        level.addFreshEntity(watcher);
        watcherUuid = watcher.getUUID();
        placeAtRing(target, watcher);
    }

    private static double rollIdealRingRadiusBlocks(ServerPlayer target) {
        RandomSource random = target.getRandom();
        int spread = Config.watcherStalkDistanceSpreadChunks;
        int center = Config.watcherStalkDistanceChunks;
        int minCh = Math.max(1, center - spread);
        int maxCh = Math.min(64, center + spread);
        int chunks = minCh + random.nextInt(maxCh - minCh + 1);
        return chunks * (double) WatcherConstants.BLOCKS_PER_CHUNK;
    }

    private static void placeAtRing(ServerPlayer target, WatcherEntity watcher) {
        ServerLevel level = (ServerLevel) target.level();
        TeleportEffects.spawnTeleportFog(level, watcher.position());
        RandomSource random = target.getRandom();
        double ringRadiusBlocks = rollIdealRingRadiusBlocks(target);
        watcher.setRingRadiusBlocks(ringRadiusBlocks);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double px = target.getX() + Math.cos(angle) * ringRadiusBlocks;
        double pz = target.getZ() + Math.sin(angle) * ringRadiusBlocks;
        BlockPos column = BlockPos.containing(px, 0.0, pz);
        level.getChunk(column.getX() >> 4, column.getZ() >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        watcher.setPos(px + 0.5, y, pz + 0.5);
        watcher.setDeltaMovement(Vec3.ZERO);
    }

    private static void teleportFarAway(ServerLevel level, ServerPlayer target, WatcherEntity watcher) {
        TeleportEffects.spawnTeleportFog(level, watcher.position());
        RandomSource random = target.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double minFarBlocks = Config.watcherFleeFarDistanceChunks * (double) WatcherConstants.BLOCKS_PER_CHUNK;
        double farBlocks = minFarBlocks;
        MinecraftServer server = level.getServer();
        if (server != null) {
            int vd = server.getPlayerList().getViewDistance();
            farBlocks = Math.max(minFarBlocks, (vd + 4) * (double) WatcherConstants.BLOCKS_PER_CHUNK);
        }
        double px = target.getX() + Math.cos(angle) * farBlocks;
        double pz = target.getZ() + Math.sin(angle) * farBlocks;
        BlockPos column = BlockPos.containing(px, 0.0, pz);
        level.getChunk(column.getX() >> 4, column.getZ() >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        watcher.setPos(px + 0.5, y, pz + 0.5);
        watcher.setDeltaMovement(Vec3.ZERO);
        watcher.setRingRadiusBlocks(rollIdealRingRadiusBlocks(target));
    }

    private static void facePlayer(WatcherEntity watcher, ServerPlayer player) {
        Vec3 vec = player.getEyePosition().subtract(watcher.getEyePosition());
        double horiz = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        if (horiz < 1.0E-4) {
            return;
        }
        float yRot = (float) (Mth.atan2(vec.z, vec.x) * (180.0 / Math.PI)) - 90.0F;
        float xRot = (float) (-(Mth.atan2(vec.y, horiz) * (180.0 / Math.PI)));
        watcher.setYRot(yRot);
        watcher.setYHeadRot(yRot);
        watcher.setYBodyRot(yRot);
        watcher.setXRot(xRot);
    }

}
