package com.qugor.shadowofthewatcher;

import com.qugor.shadowofthewatcher.entity.SceneActorEntity;
import com.qugor.shadowofthewatcher.entity.WatcherEntity;
import com.qugor.shadowofthewatcher.registry.ModEntityTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShadowOfTheWatcher.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModForgeEvents {
    private ModForgeEvents() {}

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.WATCHER.get(), WatcherEntity.createAttributes().build());
        event.put(ModEntityTypes.SCENE_ACTOR.get(), SceneActorEntity.createAttributes().build());
    }
}
