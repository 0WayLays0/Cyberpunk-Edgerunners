package com.waylays.chromeneon.common.player;

import com.waylays.chromeneon.common.cyberware.CyberwareCatalog;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The derived numbers of a body, computed on the server from the data and the config and sent as they are.
 *
 * @param ramRegenProgress ticks already accumulated toward the next RAM cell
 * @param ramRegenTicks    ticks needed per cell, so the client can animate the recharge
 * @param gameTime         server time when computed: the reference for every cooldown sent with it
 */
public record CyberStats(int humanity, int maxHumanity, int ram, int ramCapacity, int ramRegenProgress, int ramRegenTicks,
                         int capacityUsed, int capacity, long gameTime) {
    public static final StreamCodec<ByteBuf, CyberStats> STREAM_CODEC = StreamCodec.of((buf, s) -> {
        ByteBufCodecs.VAR_INT.encode(buf, s.humanity);
        ByteBufCodecs.VAR_INT.encode(buf, s.maxHumanity);
        ByteBufCodecs.VAR_INT.encode(buf, s.ram);
        ByteBufCodecs.VAR_INT.encode(buf, s.ramCapacity);
        ByteBufCodecs.VAR_INT.encode(buf, s.ramRegenProgress);
        ByteBufCodecs.VAR_INT.encode(buf, s.ramRegenTicks);
        ByteBufCodecs.VAR_INT.encode(buf, s.capacityUsed);
        ByteBufCodecs.VAR_INT.encode(buf, s.capacity);
        ByteBufCodecs.VAR_LONG.encode(buf, s.gameTime);
    }, buf -> new CyberStats(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf)));

    /** The config numbers the stats depend on. */
    public record Limits(int maxHumanity, int baseRam, int ramCap, int regenTicks, int baseCapacity) {}

    public static int ramCapacity(PlayerCyberData data, CyberwareCatalog catalog, Limits limits, long now) {
        return Math.min(limits.ramCap(), limits.baseRam() + data.loadout().ramBonus(catalog, now));
    }

    public static int capacity(PlayerCyberData data, Limits limits) {
        return limits.baseCapacity() + data.capacityBonus();
    }

    public static CyberStats of(PlayerCyberData data, CyberwareCatalog catalog, Limits limits, long now) {
        return new CyberStats(limits.maxHumanity() - data.loadout().humanityCost(catalog), limits.maxHumanity(),
                data.ram().cells(), ramCapacity(data, catalog, limits, now), data.ram().regenProgress(), limits.regenTicks(),
                data.loadout().capacityUsed(catalog), capacity(data, limits), now);
    }

    /** Humanity as a share of its maximum, 0..1. */
    public float humanityRatio() {
        return maxHumanity <= 0 ? 1F : Math.clamp(humanity / (float) maxHumanity, 0F, 1F);
    }
}
