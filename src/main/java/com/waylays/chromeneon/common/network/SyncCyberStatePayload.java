package com.waylays.chromeneon.common.network;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.cyberware.CyberwareLoadout;
import com.waylays.chromeneon.common.player.CyberStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/** Server to client: the owner's full cyber state (stats, implants, disabled implants). Small and sent on change. */
public record SyncCyberStatePayload(CyberStats stats, CyberwareLoadout loadout, Set<ResourceLocation> disabled)
        implements CustomPacketPayload {
    public static final Type<SyncCyberStatePayload> TYPE = new Type<>(ChromeNeon.id("sync_cyber_state"));

    public static final StreamCodec<ByteBuf, SyncCyberStatePayload> STREAM_CODEC = StreamCodec.composite(
            CyberStats.STREAM_CODEC, SyncCyberStatePayload::stats,
            ByteBufCodecs.fromCodec(CyberwareLoadout.CODEC), SyncCyberStatePayload::loadout,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), p -> new HashSet<>(p.disabled()),
            SyncCyberStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
