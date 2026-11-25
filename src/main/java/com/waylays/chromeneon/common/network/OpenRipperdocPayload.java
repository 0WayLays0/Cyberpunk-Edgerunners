package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.ChromeNeon;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the player sat down in the ripperdoc chair at {@code chair}: open the surgery interface. */
public record OpenRipperdocPayload(BlockPos chair) implements CustomPacketPayload {
    public static final Type<OpenRipperdocPayload> TYPE = new Type<>(ChromeNeon.id("open_ripperdoc"));
    public static final StreamCodec<ByteBuf, OpenRipperdocPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(OpenRipperdocPayload::new, OpenRipperdocPayload::chair);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
