package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.ChromeNeon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to the players who see someone: what of that player's cyberware is visible (blades out on either arm).
 * Their own client already knows; this lets everyone else draw it.
 */
public record ImplantVisualsPayload(int entityId, int flags) implements CustomPacketPayload {
    public static final int LEFT_BLADE = 1, RIGHT_BLADE = 2, DILATING = 4;

    public static final Type<ImplantVisualsPayload> TYPE = new Type<>(ChromeNeon.id("implant_visuals"));
    public static final StreamCodec<ByteBuf, ImplantVisualsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ImplantVisualsPayload::entityId,
            ByteBufCodecs.VAR_INT, ImplantVisualsPayload::flags,
            ImplantVisualsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
