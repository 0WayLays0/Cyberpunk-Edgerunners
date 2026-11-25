package com.waylays.chromeneon.common.hud;

import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: show a notification on the player's HUD. Built by {@link HudMessages}. */
public record HudNotifyPayload(NotificationKind kind, Component title, Component message, int durationTicks)
        implements CustomPacketPayload {
    public static final Type<HudNotifyPayload> TYPE = new Type<>(ChromeNeon.id("hud_notify"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HudNotifyPayload> STREAM_CODEC = StreamCodec.composite(
            NotificationKind.STREAM_CODEC, HudNotifyPayload::kind,
            ComponentSerialization.STREAM_CODEC, HudNotifyPayload::title,
            ComponentSerialization.STREAM_CODEC, HudNotifyPayload::message,
            ByteBufCodecs.VAR_INT, HudNotifyPayload::durationTicks,
            HudNotifyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
