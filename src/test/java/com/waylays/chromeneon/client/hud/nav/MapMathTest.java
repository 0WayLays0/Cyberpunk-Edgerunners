package com.waylays.chromeneon.client.hud.nav;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapMathTest {
    @Test
    void northUpPutsNorthAtTheTopAndEastOnTheRight() {
        MapView v = new MapView(100, 200, 37F, false, 1F, 72);
        assertArrayEquals(new float[] {0F, -10F}, v.toMap(100, 190), 1e-4F);
        assertArrayEquals(new float[] {10F, 0F}, v.toMap(110, 200), 1e-4F);
    }

    @Test
    void rotatingPutsWhatThePlayerFacesAtTheTop() {
        // yaw 0 faces south (+z), yaw 90 faces west (-x)
        assertArrayEquals(new float[] {0F, -8F}, new MapView(0, 0, 0F, true, 1F, 72).toMap(0, 8), 1e-4F);
        assertArrayEquals(new float[] {0F, -8F}, new MapView(0, 0, 90F, true, 1F, 72).toMap(-8, 0), 1e-4F);
        // Facing south, west (-x) is on the right
        assertArrayEquals(new float[] {8F, 0F}, new MapView(0, 0, 0F, true, 1F, 72).toMap(-8, 0), 1e-4F);
    }

    @Test
    void toWorldInvertsToMapWithZoom() {
        MapView v = new MapView(-31.5, 12.25, 213F, true, 2F, 72);
        float[] m = v.toMap(-10, 40);
        double[] w = v.toWorld(m[0], m[1]);
        assertEquals(-10, w[0], 1e-3);
        assertEquals(40, w[1], 1e-3);
        assertEquals(5F, new MapView(0, 0, 0F, false, 2F, 72).toMap(10, 0)[0], 1e-4F, "2 blocks per pixel");
    }

    @Test
    void axesMatchToWorld() {
        MapView v = new MapView(12, -40, 71F, true, 1.5F, 72);
        double[] a = v.axes();
        double[] w = v.toWorld(7, -3);
        assertEquals(12 + a[0] * 7 - a[2] * -3, w[0], 1e-6);
        assertEquals(-40 + a[1] * 7 - a[3] * -3, w[1], 1e-6);
    }

    @Test
    void reliefContoursAndPalette() {
        int base = MapPalette.land(0x707070);
        assertTrue(MapPalette.luminance(MapPalette.relief(base, 70, 60, 60)) > MapPalette.luminance(base), "rising ground is lit");
        assertTrue(MapPalette.luminance(MapPalette.relief(base, 60, 70, 70)) < MapPalette.luminance(base), "falling ground is shaded");
        assertTrue(MapPalette.contour(64, 63, 64), "crossing 64 draws a line");
        assertFalse(MapPalette.contour(66, 65, 67), "same 8-block band: no line");
        assertTrue(MapPalette.luminance(MapPalette.water(0)) > MapPalette.luminance(MapPalette.water(8)), "deep water is darker");
        assertTrue(MapPalette.luminance(MapPalette.land(0xFFFFFF)) > MapPalette.luminance(MapPalette.land(0x202020)));
    }

    @Test
    void frameMaskCutsTwoCorners() {
        assertFalse(MinimapDisplay.inside(0, 0, 72), "top-left cut");
        assertFalse(MinimapDisplay.inside(71, 71, 72), "bottom-right cut");
        assertTrue(MinimapDisplay.inside(71, 0, 72), "top-right square");
        assertTrue(MinimapDisplay.inside(0, 71, 72), "bottom-left square");
        assertTrue(MinimapDisplay.inside(36, 36, 72));
    }

    @Test
    void clockReadsMinecraftTime() {
        assertArrayEquals(new int[] {6, 0}, ClockDisplay.time(0));
        assertArrayEquals(new int[] {12, 0}, ClockDisplay.time(6000));
        assertArrayEquals(new int[] {0, 0}, ClockDisplay.time(18000));
        assertArrayEquals(new int[] {5, 30}, ClockDisplay.time(23500));
        assertArrayEquals(new int[] {12, 0}, ClockDisplay.time(24000 * 5 + 6000), "any day");
    }
}
