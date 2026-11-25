package com.waylays.chromeneon.client.hud.theme;

/**
 * Every colour of Chrome Néon's interface, ARGB. Roles are fixed: violet is the system (frames, health), cyan is
 * data (RAM, information, secondary values), white is what must be read first, orange and red are danger.
 */
public final class NeonPalette {
    public static final int VOID = 0xFF06040C;
    public static final int PANEL = 0xFF0E0A18;
    public static final int VIOLET = 0xFFB026FF;
    public static final int VIOLET_HOT = 0xFFD883FF;
    public static final int VIOLET_DEEP = 0xFF3B1260;
    public static final int CYAN = 0xFF1EF2FF;
    public static final int CYAN_SOFT = 0xFF7FD9E6;
    public static final int CYAN_DEEP = 0xFF0A3B47;
    public static final int WHITE = 0xFFF5F2FF;
    public static final int LAVENDER = 0xFFC9C0E0;
    public static final int STEEL = 0xFFE3DEF2;
    public static final int ORANGE = 0xFFFF8A1F;
    public static final int RED = 0xFFFF2B4E;
    public static final int RED_DEEP = 0xFF4A0A18;

    private NeonPalette() {}

    /** The same colour with its alpha multiplied by {@code factor} (0..1). */
    public static int fade(int argb, float factor) {
        float f = Math.clamp(factor, 0F, 1F);
        int alpha = Math.round(((argb >>> 24) & 0xFF) * f);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** The colour with an absolute alpha (0..1). */
    public static int alpha(int argb, float alpha) {
        return (Math.round(Math.clamp(alpha, 0F, 1F) * 255) << 24) | (argb & 0x00FFFFFF);
    }

    /** Linear blend of two colours, channels and alpha, {@code t} in 0..1. */
    public static int lerp(int from, int to, float t) {
        float k = Math.clamp(t, 0F, 1F);
        int a = channel(from, 24, to, k), r = channel(from, 16, to, k), g = channel(from, 8, to, k), b = channel(from, 0, to, k);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int shift, int to, float k) {
        int x = (from >>> shift) & 0xFF, y = (to >>> shift) & 0xFF;
        return Math.round(x + (y - x) * k);
    }

    public static float red(int argb) {
        return ((argb >>> 16) & 0xFF) / 255F;
    }

    public static float green(int argb) {
        return ((argb >>> 8) & 0xFF) / 255F;
    }

    public static float blue(int argb) {
        return (argb & 0xFF) / 255F;
    }

    public static float alphaOf(int argb) {
        return ((argb >>> 24) & 0xFF) / 255F;
    }
}
