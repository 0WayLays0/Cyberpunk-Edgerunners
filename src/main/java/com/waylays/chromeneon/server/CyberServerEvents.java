package com.waylays.chromeneon.server;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.player.CyberData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Keeps each player's cyber state alive on the server: RAM regeneration and syncing the owner's client. */
@EventBusSubscriber(modid = ChromeNeon.MODID)
public final class CyberServerEvents {
    private CyberServerEvents() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive()) {
            boolean changed = CyberData.get(player).ram()
                    .tick(CyberData.ramCapacity(player), ServerConfig.RAM_REGEN_TICKS.get());
            if (changed) CyberData.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CyberData.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CyberData.sync(player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CyberData.sync(player);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        ChromeCommands.register(event.getDispatcher());
    }
}
