package com.qugor.shadowofthewatcher.scene;

import com.qugor.shadowofthewatcher.TeleportEffects;
import com.qugor.shadowofthewatcher.entity.SceneActorEntity;
import com.qugor.shadowofthewatcher.registry.ModEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class SceneActorTeleport {
    private SceneActorTeleport() {}

    public static void teleport(ServerLevel level, SceneActorEntity entity, double x, double y, double z) {
        TeleportEffects.spawnTeleportFog(level, entity.position());
        entity.setPos(x, y, z);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    public static void teleport(ServerLevel level, SceneActorEntity entity, Vec3 pos) {
        teleport(level, entity, pos.x, pos.y, pos.z);
    }

    public static SceneActorEntity spawn(ServerLevel level, double x, double y, double z) {
        SceneActorEntity entity = ModEntityTypes.SCENE_ACTOR.get().create(level);
        if (entity == null) {
            throw new IllegalStateException("scene_actor");
        }
        entity.setPos(x, y, z);
        level.addFreshEntity(entity);
        TeleportEffects.spawnTeleportFog(level, new Vec3(x, y, z));
        return entity;
    }
}
