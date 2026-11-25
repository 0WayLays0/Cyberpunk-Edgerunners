package com.waylays.chromeneon.common.player;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.CyberwareCatalog;
import com.waylays.chromeneon.common.network.SyncCyberStatePayload;
import com.waylays.chromeneon.common.registry.ModAttachments;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side access to a player's cyber data: reading it, computing its stats and sending it to its owner. */
public final class CyberData {
    private CyberData() {}

    public static PlayerCyberData get(Player player) {
        return player.getData(ModAttachments.CYBER_DATA);
    }

    public static CyberStats.Limits limits(ServerPlayer player) {
        return new CyberStats.Limits(ServerConfig.MAX_HUMANITY.get(), ServerConfig.BASE_RAM.get(), ServerConfig.RAM_CAP.get(),
                regenTicks(player), ServerConfig.BASE_CAPACITY.get());
    }

    /** RAM recovery time per cell, shortened by a working coprocessor. */
    public static int regenTicks(ServerPlayer player) {
        int base = ServerConfig.RAM_REGEN_TICKS.get();
        return Cyberware.working(player, ModImplants.SYNAPSE_COPROCESSOR.get())
                ? Math.max(1, (int) Math.round(base * ServerConfig.COPROCESSOR_REGEN.get())) : base;
    }

    public static CyberStats stats(ServerPlayer player) {
        return CyberStats.of(get(player), CyberwareCatalog.REGISTRIES, limits(player), player.level().getGameTime());
    }

    public static int ramCapacity(ServerPlayer player) {
        return CyberStats.ramCapacity(get(player), CyberwareCatalog.REGISTRIES, limits(player), player.level().getGameTime());
    }

    /**
     * Sends the owner its current state. Call after any change the client must show. Skipped for connections that
     * did not negotiate the mod's channel (fake players, test players), which would otherwise throw.
     */
    public static void sync(ServerPlayer player) {
        if (player.connection == null || !player.connection.hasChannel(SyncCyberStatePayload.TYPE)) return;
        PlayerCyberData data = get(player);
        PacketDistributor.sendToPlayer(player, new SyncCyberStatePayload(stats(player), data.loadout().copy()));
    }
}
