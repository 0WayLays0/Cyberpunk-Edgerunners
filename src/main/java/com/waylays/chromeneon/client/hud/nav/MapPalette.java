package com.waylays.chromeneon.client.hud.nav;

/**
 * The tactical look of the minimap: terrain is not drawn in its real colours but as violet brightness (from the
 * block's map colour), water in deep cyan by depth, lava in orange (a danger), relief by light from the north-west
 * and contour lines every 8 blocks of height. Pure functions, ARGB.
 */
public final class MapPalette {
    static final int LAND_DARK = 0xFF0F0A1E, LAND_LIGHT = 0xFF6A47A8;
    static final int WATER_SHALLOW = 0xFF0F6273, WATER_DEEP = 0xFF061F29;
    static final int LAVA = 0xFFE0661C;
    static final int CONTOUR = 0xFFD883FF;
    static final int NO_DATA = 0xFF07050D, NO_DATA_LINE = 0xFF120D20;

    private MapPalette() {}

    /** Perceived brightness of an RGB colour, 0..1. */
    static float luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return (0.299F * r + 0.587F * g + 0.114F * b) / 255F;
    }

    public static int land(int mapRgb) {
        return lerp(LAND_DARK, LAND_LIGHT, (float) Math.pow(luminance(mapRgb), 0.9));
    }

    public static int water(int depth) {
        return lerp(WATER_SHALLOW, WATER_DEEP, Math.min(1F, depth / 8F));
    }

    public static int lava() {
        return LAVA;
    }

    /** Relief: brighter where the ground rises toward the south-east, darker where it falls. */
    public static int relief(int argb, int height, int north, int west) {
        float slope = (height - north) + (height - west);
        float k = 1F + Math.clamp(slope * 0.09F, -0.4F, 0.4F);
        return scale(argb, k);
    }

    /** True where a contour line (every 8 blocks of height) passes between this column and its neighbours. */
    public static boolean contour(int height, int north, int west) {
        int band = Math.floorDiv(height, 8);
        return band != Math.floorDiv(north, 8) || band != Math.floorDiv(west, 8);
    }

    public static int withContour(int argb) {
        return lerp(argb, CONTOUR, 0.38F);
    }

    static int scale(int argb, float k) {
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * k));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * k));
        int b = Math.min(255, Math.round((argb & 0xFF) * k));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    static int lerp(int a, int b, float t) {
        float k = Math.clamp(t, 0F, 1F);
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * k);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * k);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
