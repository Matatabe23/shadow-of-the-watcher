package com.qugor.shadowofthewatcher.client;

import com.qugor.shadowofthewatcher.ShadowOfTheWatcher;
import com.qugor.shadowofthewatcher.entity.WatcherEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WatcherRenderer extends HumanoidMobRenderer<WatcherEntity, PlayerModel<WatcherEntity>> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(ShadowOfTheWatcher.MOD_ID, "textures/entity/watcher.png");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(WatcherEntity entity) {
        return TEXTURE;
    }
}
