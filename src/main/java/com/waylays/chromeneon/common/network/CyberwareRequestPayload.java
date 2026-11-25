package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.ChromeNeon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;

import java.util.function.IntFunction;

/**
 * Client to server: a request about the player's own cyberware from the interfaces. The server checks it in full
 * (chair, combat, item, rules) before doing anything.
 *
 * @param index         the socket, for removal and switching
 * @param inventorySlot the implant item, for installation
 */
public record CyberwareRequestPayload(Action action, ResourceLocation slot, int index, int inventorySlot, boolean flag)
        implements CustomPacketPayload {
    public enum Action {
        INSTALL, REMOVE, SET_ENABLED;

        static final IntFunction<Action> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        static final StreamCodec<ByteBuf, Action> CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    }

    public static final Type<CyberwareRequestPayload> TYPE = new Type<>(ChromeNeon.id("cyberware_request"));
    public static final StreamCodec<ByteBuf, CyberwareRequestPayload> STREAM_CODEC = StreamCodec.composite(
            Action.CODEC, CyberwareRequestPayload::action,
            ResourceLocation.STREAM_CODEC, CyberwareRequestPayload::slot,
            ByteBufCodecs.VAR_INT, CyberwareRequestPayload::index,
            ByteBufCodecs.VAR_INT, CyberwareRequestPayload::inventorySlot,
            ByteBufCodecs.BOOL, CyberwareRequestPayload::flag,
            CyberwareRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
