package com.waylays.chromeneon.client.hud.vitals;

import com.waylays.chromeneon.client.hud.HudTime;
import com.waylays.chromeneon.common.network.SyncCyberStatePayload;
import com.waylays.chromeneon.common.player.SyncedCyberState;

import java.util.Arrays;

/**
 * The RAM as the HUD shows it: cells from the server's state, plus the visual events around them (cells spent,
 * cells added, a request for more RAM than available). The RAM mechanics themselves live on the server.
 */
public final class RamTracker {
    static final float INSUFFICIENT_TICKS = 24F;
    private int lastRam = -1, lastCapacity = -1;
    final float[] spentAt = new float[64];
    final float[] addedAt = new float[64];
    float insufficientAt = -1000F;
    int insufficientCost;

    public RamTracker() {
        Arrays.fill(spentAt, -1000F);
        Arrays.fill(addedAt, -1000F);
    }

    public void tick() {
        SyncCyberStatePayload state = SyncedCyberState.current();
        if (state == null) {
            lastRam = lastCapacity = -1;
            return;
        }
        int ram = state.stats().ram(), cap = Math.min(64, state.stats().ramCapacity());
        float now = HudTime.ticks();
        if (lastRam >= 0 && ram < lastRam) {
            for (int i = ram; i < Math.min(lastRam, 64); i++) spentAt[i] = now;
        }
        if (lastCapacity >= 0 && cap > lastCapacity) {
            for (int i = lastCapacity; i < cap; i++) addedAt[i] = now;
        }
        lastRam = ram;
        lastCapacity = cap;
    }

    /** Shows that an action needing {@code cost} cells could not run: the missing cells flash red. */
    public void insufficient(int cost) {
        insufficientAt = HudTime.ticks();
        insufficientCost = cost;
    }
}
