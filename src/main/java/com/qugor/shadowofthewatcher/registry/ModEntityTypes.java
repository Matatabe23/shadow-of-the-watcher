package com.qugor.shadowofthewatcher.registry;

import com.qugor.shadowofthewatcher.ShadowOfTheWatcher;
import com.qugor.shadowofthewatcher.entity.WatcherEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ShadowOfTheWatcher.MOD_ID);

    public static final RegistryObject<EntityType<WatcherEntity>> WATCHER = ENTITY_TYPES.register("watcher",
        () -> EntityType.Builder.of(WatcherEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .build(ResourceLocation.fromNamespaceAndPath(ShadowOfTheWatcher.MOD_ID, "watcher").toString()));

    private ModEntityTypes() {}
}
