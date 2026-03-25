package com.qugor.shadowofthewatcher;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(ShadowOfTheWatcher.MOD_ID)
public final class ShadowOfTheWatcher {
    public static final String MOD_ID = "shadow_of_the_watcher";

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<Block> SHADOW_BLOCK = BLOCKS.register("shadow_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("shadow_block"))
            .mapColor(MapColor.STONE)
        )
    );

    public static final RegistryObject<Item> SHADOW_BLOCK_ITEM = ITEMS.register("shadow_block",
        () -> new BlockItem(SHADOW_BLOCK.get(), new Item.Properties().setId(ITEMS.key("shadow_block")))
    );

    public static final RegistryObject<Item> WATCHER_ITEM = ITEMS.register("watcher_item",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("watcher_item"))
            .food(new FoodProperties.Builder()
                .alwaysEdible()
                .nutrition(1)
                .saturationModifier(2f)
                .build()
            )
        )
    );

    public static final RegistryObject<CreativeModeTab> WATCHER_TAB = CREATIVE_MODE_TABS.register("watcher_tab",
        () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> WATCHER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(WATCHER_ITEM.get()))
            .build()
    );

    public ShadowOfTheWatcher(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        BLOCKS.register(modBusGroup);
        ITEMS.register(modBusGroup);
        CREATIVE_MODE_TABS.register(modBusGroup);

        BuildCreativeModeTabContentsEvent.BUS.addListener(ShadowOfTheWatcher::addCreative);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        if (Config.logShadowBlock) {
            LOGGER.info("SHADOW BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(SHADOW_BLOCK.get()));
        }
        LOGGER.info("{}{}", Config.watcherPowerIntroduction, Config.watcherPower);
        Config.items.forEach(item -> LOGGER.info("ITEM >> {}", item));
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(SHADOW_BLOCK_ITEM);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}

