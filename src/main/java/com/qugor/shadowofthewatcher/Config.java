package com.qugor.shadowofthewatcher;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = ShadowOfTheWatcher.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_SHADOW_BLOCK = BUILDER
        .define("logShadowBlock", true);

    private static final ForgeConfigSpec.IntValue WATCHER_POWER = BUILDER
        .defineInRange("watcherPower", 42, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue WATCHER_STALK_DISTANCE_CHUNKS = BUILDER
        .defineInRange("watcherStalkDistanceChunks", 6, 1, 64);

    private static final ForgeConfigSpec.IntValue WATCHER_STALK_DISTANCE_SPREAD_CHUNKS = BUILDER
        .defineInRange("watcherStalkDistanceSpreadChunks", 0, 0, 16);

    private static final ForgeConfigSpec.IntValue WATCHER_VANISH_WHEN_CLOSER_THAN_CHUNKS = BUILDER
        .defineInRange("watcherVanishWhenCloserThanChunks", 4, 1, 64);

    private static final ForgeConfigSpec.IntValue WATCHER_FLEE_FAR_DISTANCE_CHUNKS = BUILDER
        .defineInRange("watcherFleeFarDistanceChunks", 14, 4, 64);

    private static final ForgeConfigSpec.IntValue WATCHER_FLEE_COOLDOWN_TICKS = BUILDER
        .defineInRange("watcherFleeCooldownTicks", 60, 0, 600);

    private static final ForgeConfigSpec.IntValue WATCHER_PURSUIT_MARGIN_BLOCKS = BUILDER
        .defineInRange("watcherPursuitMarginBlocks", 16, 1, 128);

    private static final ForgeConfigSpec.IntValue WATCHER_TARGET_SWITCH_MINUTES = BUILDER
        .defineInRange("watcherTargetSwitchMinutes", 5, 1, 10080);

    private static final ForgeConfigSpec.ConfigValue<String> WATCHER_POWER_INTRODUCTION = BUILDER
        .define("watcherPowerIntroduction", "Watcher power is... ");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
        .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logShadowBlock;
    public static int watcherPower;
    public static int watcherStalkDistanceChunks;
    public static int watcherStalkDistanceSpreadChunks;
    public static int watcherVanishWhenCloserThanChunks;
    public static int watcherFleeFarDistanceChunks;
    public static int watcherFleeCooldownTicks;
    public static int watcherPursuitMarginBlocks;
    public static int watcherTargetSwitchMinutes;
    public static String watcherPowerIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof final String itemName)) return false;
        final ResourceLocation id = ResourceLocation.tryParse(itemName);
        return id != null && ForgeRegistries.ITEMS.containsKey(id);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logShadowBlock = LOG_SHADOW_BLOCK.get();
        watcherPower = WATCHER_POWER.get();
        watcherStalkDistanceChunks = WATCHER_STALK_DISTANCE_CHUNKS.get();
        watcherStalkDistanceSpreadChunks = WATCHER_STALK_DISTANCE_SPREAD_CHUNKS.get();
        watcherVanishWhenCloserThanChunks = WATCHER_VANISH_WHEN_CLOSER_THAN_CHUNKS.get();
        if (watcherVanishWhenCloserThanChunks >= watcherStalkDistanceChunks) {
            watcherVanishWhenCloserThanChunks = Math.max(1, watcherStalkDistanceChunks - 1);
        }
        watcherFleeFarDistanceChunks = WATCHER_FLEE_FAR_DISTANCE_CHUNKS.get();
        watcherFleeCooldownTicks = WATCHER_FLEE_COOLDOWN_TICKS.get();
        watcherPursuitMarginBlocks = WATCHER_PURSUIT_MARGIN_BLOCKS.get();
        watcherTargetSwitchMinutes = WATCHER_TARGET_SWITCH_MINUTES.get();
        watcherPowerIntroduction = WATCHER_POWER_INTRODUCTION.get();

        items = ITEM_STRINGS.get().stream()
            .map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
            .collect(Collectors.toSet());
    }
}

