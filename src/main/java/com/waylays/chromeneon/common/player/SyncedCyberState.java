package com.waylays.chromeneon.common.player;

import com.waylays.chromeneon.common.network.SyncCyberStatePayload;

/**
 * The last cyber state the server sent to this client. Holds no client classes (it is reached from the common
 * payload handler) but is only ever filled on the physical client; the HUD reads it.
 */
public final class SyncedCyberState {
    private static volatile SyncCyberStatePayload current;

    private SyncedCyberState() {}

    public static void accept(SyncCyberStatePayload state) {
        current = state;
    }

    public static void clear() {
        current = null;
    }

    /** Null until the server has sent the first state (e.g. on a server without the mod's data yet). */
    public static SyncCyberStatePayload current() {
        return current;
    }
}
