package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.ChromeNeon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to the players who see someone, and to that player: what of their cyberware shows (blades out on either
 * arm) and the slowdown of the time dilation they run (0 = none), so every client draws the blades and the
 * afterimages and slows the same entities.
 */
public record ImplantVisualsPayload(int entityId, int flags, int slowdown) implements CustomPacketPayload {
    public static final int LEFT_BLADE = 1, RIGHT_BLADE = 2;

    public static final Type<ImplantVisualsPayload> TYPE = new Type<>(ChromeNeon.id("implant_visuals"));
    public static final StreamCodec<ByteBuf, ImplantVisualsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ImplantVisualsPayload::entityId,
            ByteBufCodecs.VAR_INT, ImplantVisualsPayload::flags,
            ByteBufCodecs.VAR_INT, ImplantVisualsPayload::slowdown,
            ImplantVisualsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
