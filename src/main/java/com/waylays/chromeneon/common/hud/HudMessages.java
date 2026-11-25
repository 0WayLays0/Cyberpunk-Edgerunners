package com.waylays.chromeneon.common.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side entry point to a player's HUD. Every system of the mod that needs to tell the player something
 * (cyberware, hacks, calls...) goes through here; the client decides how it looks.
 */
public final class HudMessages {
    private HudMessages() {}

    public static void notify(ServerPlayer player, NotificationKind kind, Component title, Component message) {
        notify(player, kind, title, message, kind.defaultDuration());
    }

    public static void notify(ServerPlayer player, NotificationKind kind, Component title, Component message, int durationTicks) {
        if (player.connection == null || !player.connection.hasChannel(HudNotifyPayload.TYPE)) return;
        PacketDistributor.sendToPlayer(player, new HudNotifyPayload(kind, title, message, Math.max(20, durationTicks)));
    }
}
