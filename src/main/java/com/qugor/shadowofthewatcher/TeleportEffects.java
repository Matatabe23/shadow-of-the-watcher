package com.qugor.shadowofthewatcher;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class TeleportEffects {
    private TeleportEffects() {}

    public static void spawnTeleportFog(ServerLevel level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y + 1.0;
        double z = pos.z;
        for (int i = 0; i < 72; i++) {
            double ox = level.random.nextGaussian() * 0.6;
            double oy = level.random.nextGaussian() * 0.9;
            double oz = level.random.nextGaussian() * 0.6;
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + ox, y + oy, z + oz, 1, 0.0, 0.06, 0.0, 0.02);
            level.sendParticles(ParticleTypes.CLOUD, x + ox, y + oy, z + oz, 1, 0.0, 0.05, 0.0, 0.02);
        }
    }
}
