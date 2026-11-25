package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.ChromeNeon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the outcome of a request or a key press, as a translation key ({@code chromeneon.install.*} or
 * {@code chromeneon.use.*}), with the RAM it needed when RAM was short.
 */
public record ActionFeedbackPayload(String key, boolean ok, int ramNeeded) implements CustomPacketPayload {
    public static final Type<ActionFeedbackPayload> TYPE = new Type<>(ChromeNeon.id("action_feedback"));
    public static final StreamCodec<ByteBuf, ActionFeedbackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ActionFeedbackPayload::key,
            ByteBufCodecs.BOOL, ActionFeedbackPayload::ok,
            ByteBufCodecs.VAR_INT, ActionFeedbackPayload::ramNeeded,
            ActionFeedbackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
