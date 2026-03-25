package com.qugor.shadowofthewatcher;

import com.qugor.shadowofthewatcher.entity.WatcherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class WatcherSpawnPlacement {
    private static final int RING_ANGLE_ATTEMPTS = 48;
    private static final int FAR_ANGLE_ATTEMPTS = 64;
    private static final int SAMPLE_ATTEMPTS_PER_PLAYER = 16;

    private WatcherSpawnPlacement() {}

    public static boolean playerHasValidRingSample(ServerLevel level, ServerPlayer player, double ringRadiusBlocks) {
        double minH = ringHorizontalMin(ringRadiusBlocks);
        double maxH = ringHorizontalMax(ringRadiusBlocks);
        RandomSource random = player.getRandom();
        for (int i = 0; i < SAMPLE_ATTEMPTS_PER_PLAYER; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double px = player.getX() + Math.cos(angle) * ringRadiusBlocks;
            double pz = player.getZ() + Math.sin(angle) * ringRadiusBlocks;
            if (findFootPosition(level, px, pz, player, minH, maxH).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<Vec3> findFootPosition(ServerLevel level, double px, double pz, ServerPlayer player,
                                                  double minHorizontalBlocks, double maxHorizontalBlocks) {
        Optional<Vec3> raw = findFootPositionUnchecked(level, px, pz);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        Vec3 v = raw.get();
        double distH = horizontalDistanceXZ(player, v.x, v.z);
        if (distH < minHorizontalBlocks - 1e-3 || distH > maxHorizontalBlocks + 1e-3) {
            return Optional.empty();
        }
        return raw;
    }

    private static Optional<Vec3> findFootPositionUnchecked(ServerLevel level, double px, double pz) {
        int ix = Mth.floor(px);
        int iz = Mth.floor(pz);
        int cx = SectionPos.blockToSectionCoord(ix);
        int cz = SectionPos.blockToSectionCoord(iz);
        if (!level.hasChunk(cx, cz)) {
            level.getChunk(cx, cz);
        }
        int minY = level.getMinBuildHeight();
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
        for (int y = surfaceY; y >= minY; y--) {
            BlockPos ground = new BlockPos(ix, y, iz);
            BlockState groundState = level.getBlockState(ground);
            if (groundState.isAir()) {
                continue;
            }
            if (!isAcceptableGround(groundState)) {
                continue;
            }
            BlockPos feet = ground.above();
            BlockState feetState = level.getBlockState(feet);
            if (!isAcceptableFeet(feetState)) {
                continue;
            }
            return Optional.of(new Vec3(px, feet.getY() + 0.01D, pz));
        }
        return Optional.empty();
    }

    public static boolean tryPlaceOntoRing(ServerLevel level, ServerPlayer target, WatcherEntity watcher, double ringRadiusBlocks) {
        double minH = ringHorizontalMin(ringRadiusBlocks);
        double maxH = ringHorizontalMax(ringRadiusBlocks);
        RandomSource random = target.getRandom();
        for (int attempt = 0; attempt < RING_ANGLE_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double px = target.getX() + Math.cos(angle) * ringRadiusBlocks;
            double pz = target.getZ() + Math.sin(angle) * ringRadiusBlocks;
            Optional<Vec3> foot = findFootPosition(level, px, pz, target, minH, maxH);
            if (foot.isPresent()) {
                TeleportEffects.spawnTeleportFog(level, watcher.position());
                Vec3 v = foot.get();
                watcher.setPos(v.x, v.y, v.z);
                watcher.setDeltaMovement(Vec3.ZERO);
                return true;
            }
        }
        return false;
    }

    public static boolean tryTeleportFar(ServerLevel level, ServerPlayer target, WatcherEntity watcher, double farBlocks) {
        double stalkMax = stalkAbsoluteMaxBlocks();
        double minH = Math.max(farBlocks * 0.88, stalkMax + 16.0);
        double maxH = farBlocks * 1.12;
        if (minH > maxH) {
            maxH = minH + 48.0;
        }
        RandomSource random = target.getRandom();
        for (int attempt = 0; attempt < FAR_ANGLE_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double px = target.getX() + Math.cos(angle) * farBlocks;
            double pz = target.getZ() + Math.sin(angle) * farBlocks;
            Optional<Vec3> foot = findFootPosition(level, px, pz, target, minH, maxH);
            if (foot.isPresent()) {
                TeleportEffects.spawnTeleportFog(level, watcher.position());
                Vec3 v = foot.get();
                watcher.setPos(v.x, v.y, v.z);
                watcher.setDeltaMovement(Vec3.ZERO);
                return true;
            }
        }
        return false;
    }

    private static double ringHorizontalMin(double ringRadiusBlocks) {
        double absMin = stalkAbsoluteMinBlocks();
        return Math.max(absMin, ringRadiusBlocks * 0.88);
    }

    private static double ringHorizontalMax(double ringRadiusBlocks) {
        double absMax = stalkAbsoluteMaxBlocks();
        return Math.min(absMax, ringRadiusBlocks * 1.12);
    }

    private static double stalkAbsoluteMinBlocks() {
        int center = Config.watcherStalkDistanceChunks;
        int spread = Config.watcherStalkDistanceSpreadChunks;
        int minCh = Math.max(1, center - spread);
        return minCh * (double) WatcherConstants.BLOCKS_PER_CHUNK;
    }

    private static double stalkAbsoluteMaxBlocks() {
        int center = Config.watcherStalkDistanceChunks;
        int spread = Config.watcherStalkDistanceSpreadChunks;
        int maxCh = Math.min(64, center + spread);
        return maxCh * (double) WatcherConstants.BLOCKS_PER_CHUNK;
    }

    private static double horizontalDistanceXZ(ServerPlayer player, double x, double z) {
        double dx = x - player.getX();
        double dz = z - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean isAcceptableGround(BlockState ground) {
        if (ground.is(Blocks.WATER) || ground.is(Blocks.LAVA) || ground.is(Blocks.BUBBLE_COLUMN)) {
            return false;
        }
        if (ground.is(Blocks.POWDER_SNOW)) {
            return false;
        }
        if (!ground.getFluidState().isEmpty()) {
            if (ground.getFluidState().is(FluidTags.WATER) || ground.getFluidState().is(FluidTags.LAVA)) {
                return false;
            }
        }
        return ground.blocksMotion();
    }

    private static boolean isAcceptableFeet(BlockState feet) {
        if (feet.is(Blocks.WATER) || feet.is(Blocks.LAVA) || feet.is(Blocks.FIRE)) {
            return false;
        }
        if (feet.is(Blocks.SWEET_BERRY_BUSH) || feet.is(Blocks.CACTUS)) {
            return false;
        }
        if (!feet.getFluidState().isEmpty()) {
            return false;
        }
        if (feet.blocksMotion()) {
            return false;
        }
        return true;
    }
}
