package com.waylays.chromeneon.client.hud;

/** The HUD's clock, in ticks: advances with the game (stops when it is paused) and interpolates between ticks. */
public final class HudTime {
    private static long ticks;

    private HudTime() {}

    public static void tick() {
        ticks++;
    }

    public static long ticks() {
        return ticks;
    }

    public static float now(float partialTick) {
        return ticks + partialTick;
    }
}
