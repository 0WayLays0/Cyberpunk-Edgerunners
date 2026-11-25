package com.waylays.chromeneon.client.hud;

/** The colours of Chrome Néon's interface, ARGB. Every HUD element takes its colours from here. */
public final class NeonPalette {
    public static final int VOID = 0xFF07060D;
    public static final int PANEL = 0xFF120E1F;
    public static final int VIOLET = 0xFFB026FF;
    public static final int VIOLET_DIM = 0xFF4A1470;
    public static final int CYAN = 0xFF1EF2FF;
    public static final int CYAN_DIM = 0xFF0B4A55;
    public static final int ALERT = 0xFFFF2A5F;

    private NeonPalette() {}

    /** The same colour with its alpha multiplied by {@code opacity} (0..1). */
    public static int fade(int argb, double opacity) {
        int alpha = (int) Math.round(((argb >>> 24) & 0xFF) * Math.clamp(opacity, 0.0, 1.0));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
