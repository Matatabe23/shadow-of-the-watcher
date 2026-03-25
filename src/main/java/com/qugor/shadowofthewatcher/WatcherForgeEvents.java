package com.qugor.shadowofthewatcher;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShadowOfTheWatcher.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WatcherForgeEvents {
    private WatcherForgeEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ServerLevel overworld = event.getServer().overworld();
        if (overworld.getGameTime() % 5L != 0L) {
            return;
        }
        WatcherManager.tickOverworld(overworld);
    }

    @SubscribeEvent
    public static void onPlayerLogIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        WatcherManager.tickOverworld((ServerLevel) player.level());
    }
}
