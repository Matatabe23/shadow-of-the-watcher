package com.qugor.shadowofthewatcher.client;

import com.qugor.shadowofthewatcher.ShadowOfTheWatcher;
import com.qugor.shadowofthewatcher.registry.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShadowOfTheWatcher.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.WATCHER.get(), WatcherRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SCENE_ACTOR.get(), SceneActorRenderer::new);
    }
}
