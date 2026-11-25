package com.waylays.chromeneon.common.hud;

import com.waylays.chromeneon.common.network.ChromeNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
        ChromeNetwork.send(player, new HudNotifyPayload(kind, title, message, Math.max(20, durationTicks)));
    }
}
