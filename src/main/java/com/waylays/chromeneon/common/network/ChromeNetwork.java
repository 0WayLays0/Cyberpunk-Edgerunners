package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.common.hud.ClientHudBridge;
import com.waylays.chromeneon.common.hud.HudNotifyPayload;
import com.waylays.chromeneon.common.player.SyncedCyberState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers every payload of the mod. The protocol version must change whenever a payload's format changes, so a
 * mismatched client and server refuse to connect instead of misreading each other.
 */
public final class ChromeNetwork {
    /** 2: RAM regeneration progress in the sync payload; HUD notifications. */
    public static final String PROTOCOL_VERSION = "2";

    private ChromeNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(SyncCyberStatePayload.TYPE, SyncCyberStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SyncedCyberState.accept(payload)));
        registrar.playToClient(HudNotifyPayload.TYPE, HudNotifyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientHudBridge.deliver(payload)));
    }
}
