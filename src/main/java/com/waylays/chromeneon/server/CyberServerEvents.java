package com.waylays.chromeneon.server;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.mechanics.BladeCombat;
import com.waylays.chromeneon.common.cyberware.mechanics.Malfunctions;
import com.waylays.chromeneon.common.hud.HudMessages;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.player.CyberStats;
import com.waylays.chromeneon.common.player.PlayerCyberData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Keeps each player's cyberware alive on the server: implants tick (recharges, durations, repairs), the RAM
 * regenerates, the blades follow the hand, humanity strain is checked every second, and the owner's client and the
 * players around stay in sync.
 */
@EventBusSubscriber(modid = ChromeNeon.MODID)
public final class CyberServerEvents {
    private CyberServerEvents() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        PlayerCyberData data = CyberData.get(player);
        boolean changed = data.ram().tick(CyberData.ramCapacity(player), CyberData.regenTicks(player));
        for (ImplantContext ctx : Cyberware.all(player)) {
            Cyberware.implant(ctx).ifPresent(i -> i.tick(ctx));
        }
        BladeCombat.tick(player, data);
        long time = player.level().getGameTime() + player.getId();
        if (time % 20 == 0) Malfunctions.check(player, CyberData.stats(player));
        if (time % 5 == 0) ImplantVisuals.update(player);
        boolean dynamic = !data.loadout().entries().isEmpty() && time % 10 == 0;
        if (changed || dynamic) CyberData.sync(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CyberData.sync(player);
            CyberStats stats = CyberData.stats(player);
            HudMessages.notify(player, NotificationKind.SYSTEM_ON, Component.translatable("chromeneon.notify.boot.title"),
                    Component.translatable("chromeneon.notify.boot.message", stats.humanity(), stats.maxHumanity(), stats.ramCapacity()));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CyberwareRequests.forget(player);
            ImplantVisuals.forget(player);
        }
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
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            ImplantVisuals.showTo(viewer, target);
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        ChromeCommands.register(event.getDispatcher());
    }
}
