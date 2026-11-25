package com.waylays.chromeneon.common.player;

import com.waylays.chromeneon.common.network.SyncCyberStatePayload;
import net.minecraft.Util;

/**
 * The last cyber state the server sent to this client. Holds no client classes (it is reached from the common
 * payload handler) but is only ever filled on the physical client; the HUD reads it.
 */
public final class SyncedCyberState {
    private static volatile SyncCyberStatePayload current;
    private static volatile long receivedAt;

    private SyncedCyberState() {}

    public static void accept(SyncCyberStatePayload state) {
        current = state;
        receivedAt = Util.getMillis();
    }

    public static void clear() {
        current = null;
    }

    /** Null until the server has sent the first state (e.g. on a server without the mod's data yet). */
    public static SyncCyberStatePayload current() {
        return current;
    }

    /**
     * Progress (0..1) of the RAM cell being recovered, extrapolated from the last sync: the server only syncs when a
     * cell is gained or spent, so the client continues the count itself. 0 when the RAM is full.
     */
    public static float regenFraction() {
        SyncCyberStatePayload state = current;
        if (state == null) return 0F;
        CyberStats s = state.stats();
        if (s.ram() >= s.ramCapacity() || s.ramRegenTicks() <= 0) return 0F;
        float elapsedTicks = (Util.getMillis() - receivedAt) / 50F;
        return Math.min(1F, (s.ramRegenProgress() + elapsedTicks) / s.ramRegenTicks());
    }
}
