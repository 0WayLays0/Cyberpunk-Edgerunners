package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.common.hud.HudNotifyPayload;
import com.waylays.chromeneon.common.player.SyncedCyberState;
import com.waylays.chromeneon.server.CyberwareRequests;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers every payload of the mod. The protocol version must change whenever a payload's format changes, so a
 * mismatched client and server refuse to connect instead of misreading each other.
 */
public final class ChromeNetwork {
    /** 3: cyberware (implant states in the sync, requests, triggers, ripperdoc, feedback, visuals). */
    public static final String PROTOCOL_VERSION = "3";

    private ChromeNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(SyncCyberStatePayload.TYPE, SyncCyberStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SyncedCyberState.accept(payload)));
        registrar.playToClient(HudNotifyPayload.TYPE, HudNotifyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBridge.notifications.accept(payload)));
        registrar.playToClient(OpenRipperdocPayload.TYPE, OpenRipperdocPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBridge.openRipperdoc.accept(payload)));
        registrar.playToClient(ActionFeedbackPayload.TYPE, ActionFeedbackPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBridge.feedback.accept(payload)));
        registrar.playToClient(ImplantVisualsPayload.TYPE, ImplantVisualsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBridge.visuals.accept(payload)));

        registrar.playToServer(CyberwareRequestPayload.TYPE, CyberwareRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> CyberwareRequests.handle((ServerPlayer) context.player(), payload)));
        registrar.playToServer(ImplantTriggerPayload.TYPE, ImplantTriggerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> CyberwareRequests.handle((ServerPlayer) context.player(), payload)));
    }
}
