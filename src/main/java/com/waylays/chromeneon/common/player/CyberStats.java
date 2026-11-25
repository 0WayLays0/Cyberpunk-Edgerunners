package com.waylays.chromeneon.common.player;

import com.waylays.chromeneon.common.cyberware.Implant;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Function;

/**
 * The derived numbers of a body: computed on the server from the data and the config, sent as they are.
 *
 * @param ramRegenProgress ticks already accumulated toward the next RAM cell when this was computed
 * @param ramRegenTicks    ticks needed per cell (server config), so the client can animate the recharge
 */
public record CyberStats(int humanity, int maxHumanity, int ram, int ramCapacity, int ramRegenProgress, int ramRegenTicks) {
    public static final StreamCodec<ByteBuf, CyberStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CyberStats::humanity,
            ByteBufCodecs.VAR_INT, CyberStats::maxHumanity,
            ByteBufCodecs.VAR_INT, CyberStats::ram,
            ByteBufCodecs.VAR_INT, CyberStats::ramCapacity,
            ByteBufCodecs.VAR_INT, CyberStats::ramRegenProgress,
            ByteBufCodecs.VAR_INT, CyberStats::ramRegenTicks,
            CyberStats::new);

    /** RAM capacity: base cells plus the bonus of enabled implants, never above the cap. */
    public static int ramCapacity(PlayerCyberData data, Function<ResourceLocation, Optional<Implant>> lookup,
                                  int baseRam, int ramCap) {
        int bonus = data.loadout().all().stream()
                .filter(data::isEnabled)
                .map(lookup).flatMap(Optional::stream)
                .mapToInt(Implant::ramBonus).sum();
        return Math.min(ramCap, baseRam + bonus);
    }

    public static CyberStats of(PlayerCyberData data, Function<ResourceLocation, Optional<Implant>> lookup,
                                int maxHumanity, int baseRam, int ramCap, int regenTicks) {
        return new CyberStats(maxHumanity - data.loadout().humanityCost(lookup), maxHumanity,
                data.ram().cells(), ramCapacity(data, lookup, baseRam, ramCap), data.ram().regenProgress(), regenTicks);
    }
}
