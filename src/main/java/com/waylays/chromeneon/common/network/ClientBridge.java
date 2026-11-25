package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.common.hud.HudNotifyPayload;

import java.util.function.Consumer;

/**
 * Where server-to-client payloads go once received. Common code cannot name client classes, so the client entry
 * point plugs its handlers in here at startup; on a dedicated server nothing is plugged and nothing is received.
 */
public final class ClientBridge {
    public static volatile Consumer<HudNotifyPayload> notifications = p -> {};
    public static volatile Consumer<OpenRipperdocPayload> openRipperdoc = p -> {};
    public static volatile Consumer<ActionFeedbackPayload> feedback = p -> {};
    public static volatile Consumer<ImplantVisualsPayload> visuals = p -> {};

    private ClientBridge() {}
}
