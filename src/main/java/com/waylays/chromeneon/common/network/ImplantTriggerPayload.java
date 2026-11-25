package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.ChromeNeon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

import java.util.function.IntFunction;

/**
 * Client to server: the player pressed an implant key (or jumped in mid-air, or lunged). {@code target} is the
 * entity their optics show, for the systems that use it; the server checks it.
 */
public record ImplantTriggerPayload(Trigger trigger, int target) implements CustomPacketPayload {
    public enum Trigger {
        ACCELERATOR, BLADES, LUNGE, AIR_JUMP, OPTICS_POWER, ANALYSIS;

        static final IntFunction<Trigger> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        static final StreamCodec<ByteBuf, Trigger> CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    }

    public static final Type<ImplantTriggerPayload> TYPE = new Type<>(ChromeNeon.id("implant_trigger"));
    public static final StreamCodec<ByteBuf, ImplantTriggerPayload> STREAM_CODEC = StreamCodec.composite(
            Trigger.CODEC, ImplantTriggerPayload::trigger,
            ByteBufCodecs.VAR_INT, ImplantTriggerPayload::target,
            ImplantTriggerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
