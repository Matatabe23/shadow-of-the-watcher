package com.qugor.shadowofthewatcher.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qugor.shadowofthewatcher.entity.SceneActorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class SceneActorRenderer extends EntityRenderer<SceneActorEntity> {
    public SceneActorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SceneActorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
    }

    @Override
    public ResourceLocation getTextureLocation(SceneActorEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
    }
}
