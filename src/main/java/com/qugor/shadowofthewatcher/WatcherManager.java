package com.qugor.shadowofthewatcher;

import com.qugor.shadowofthewatcher.entity.WatcherEntity;
import com.qugor.shadowofthewatcher.registry.ModEntityTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
            spawnWatcher(level, players);
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
            if (!teleportFarAway(level, target, watcher)) {
                tryOtherPlayersOrDespawn(level, players, watcher);
            } else {
                fleeCooldownTicks = Config.watcherFleeCooldownTicks;
            }
            faceWatcherIfPresent(level, players);
            return;
        }

        if (fleeCooldownTicks > 0) {
            fleeCooldownTicks--;
            faceWatcherIfPresent(level, players);
            return;
        }

        if (distH > stalkBlocks + pursuitMargin) {
            if (!placeAtRing(target, watcher)) {
                tryOtherPlayersOrDespawn(level, players, watcher);
            }
            faceWatcherIfPresent(level, players);
            return;
        }

        faceWatcherIfPresent(level, players);
    }

    private static void faceWatcherIfPresent(ServerLevel level, List<ServerPlayer> players) {
        WatcherEntity w = findWatcher(level, watcherUuid);
        ServerPlayer t = findPlayer(players, currentTargetUuid);
        if (w != null && w.isAlive() && t != null) {
            facePlayer(w, t);
        }
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
        if (!placeAtRing(target, watcher)) {
            tryOtherPlayersOrDespawn(level, players, watcher);
        }
        faceWatcherIfPresent(level, players);
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

    private static void spawnWatcher(ServerLevel level, List<ServerPlayer> players) {
        ArrayList<ServerPlayer> copy = shufflePlayers(level, players);
        for (ServerPlayer target : copy) {
            double radius = rollIdealRingRadiusBlocks(target);
            if (!WatcherSpawnPlacement.playerHasValidRingSample(level, target, radius)) {
                continue;
            }
            WatcherEntity watcher = ModEntityTypes.WATCHER.get().create(level);
            if (watcher == null) {
                return;
            }
            watcher.setOwnerUUID(target.getUUID());
            level.addFreshEntity(watcher);
            watcherUuid = watcher.getUUID();
            currentTargetUuid = target.getUUID();
            watcher.setRingRadiusBlocks(radius);
            if (WatcherSpawnPlacement.tryPlaceOntoRing(level, target, watcher, radius)) {
                return;
            }
            clearWatcher(level);
            currentTargetUuid = null;
        }
        currentTargetUuid = null;
    }

    private static void tryOtherPlayersOrDespawn(ServerLevel level, List<ServerPlayer> players, WatcherEntity watcher) {
        UUID skip = currentTargetUuid;
        ArrayList<ServerPlayer> copy = shufflePlayers(level, players);
        for (ServerPlayer p : copy) {
            if (skip != null && p.getUUID().equals(skip)) {
                continue;
            }
            double radius = rollIdealRingRadiusBlocks(p);
            if (!WatcherSpawnPlacement.playerHasValidRingSample(level, p, radius)) {
                continue;
            }
            currentTargetUuid = p.getUUID();
            watcher.setOwnerUUID(p.getUUID());
            watcher.setRingRadiusBlocks(radius);
            if (WatcherSpawnPlacement.tryPlaceOntoRing(level, p, watcher, radius)) {
                return;
            }
        }
        clearWatcher(level);
        currentTargetUuid = null;
    }

    private static ArrayList<ServerPlayer> shufflePlayers(ServerLevel level, List<ServerPlayer> players) {
        ArrayList<ServerPlayer> copy = new ArrayList<>(players);
        RandomSource random = level.random;
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            ServerPlayer t = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, t);
        }
        return copy;
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

    private static boolean placeAtRing(ServerPlayer target, WatcherEntity watcher) {
        ServerLevel level = (ServerLevel) target.level();
        double ringRadiusBlocks = rollIdealRingRadiusBlocks(target);
        watcher.setRingRadiusBlocks(ringRadiusBlocks);
        return WatcherSpawnPlacement.tryPlaceOntoRing(level, target, watcher, ringRadiusBlocks);
    }

    private static boolean teleportFarAway(ServerLevel level, ServerPlayer target, WatcherEntity watcher) {
        double minFarBlocks = Config.watcherFleeFarDistanceChunks * (double) WatcherConstants.BLOCKS_PER_CHUNK;
        double farBlocks = minFarBlocks;
        MinecraftServer server = level.getServer();
        if (server != null) {
            int vd = server.getPlayerList().getViewDistance();
            farBlocks = Math.max(minFarBlocks, (vd + 4) * (double) WatcherConstants.BLOCKS_PER_CHUNK);
        }
        if (WatcherSpawnPlacement.tryTeleportFar(level, target, watcher, farBlocks)) {
            watcher.setRingRadiusBlocks(rollIdealRingRadiusBlocks(target));
            return true;
        }
        double ringRadiusBlocks = rollIdealRingRadiusBlocks(target);
        watcher.setRingRadiusBlocks(ringRadiusBlocks);
        return WatcherSpawnPlacement.tryPlaceOntoRing(level, target, watcher, ringRadiusBlocks);
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
