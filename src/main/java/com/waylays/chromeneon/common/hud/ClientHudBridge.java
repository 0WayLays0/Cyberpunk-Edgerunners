package com.waylays.chromeneon.common.hud;

import java.util.function.Consumer;

/**
 * Where HUD payloads go once received. Common code cannot name client classes, so the client entry point plugs its
 * HUD in here at startup; on a dedicated server nothing is plugged and nothing is ever received.
 */
public final class ClientHudBridge {
    private static volatile Consumer<HudNotifyPayload> notifications = payload -> {};

    private ClientHudBridge() {}

    public static void setNotificationSink(Consumer<HudNotifyPayload> sink) {
        notifications = sink;
    }

    public static void deliver(HudNotifyPayload payload) {
        notifications.accept(payload);
    }
}
