package com.waylays.chromeneon.client.hud.anim;

import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasingTest {
    @Test
    void curvesStartAtZeroEndAtOneAndClamp() {
        for (var curve : new java.util.function.Function[] {
                (java.util.function.Function<Float, Float>) Easing::outCubic, (java.util.function.Function<Float, Float>) Easing::inCubic,
                (java.util.function.Function<Float, Float>) Easing::inOutCubic, (java.util.function.Function<Float, Float>) Easing::smoothstep}) {
            @SuppressWarnings("unchecked") var f = (java.util.function.Function<Float, Float>) curve;
            assertEquals(0F, f.apply(0F), 1e-5F);
            assertEquals(1F, f.apply(1F), 1e-5F);
            assertEquals(1F, f.apply(3F), 1e-5F);
            assertEquals(0F, f.apply(-2F), 1e-5F);
        }
        assertTrue(Easing.outBack(0.7F) > 1F, "overshoots before settling");
        assertEquals(0.5F, Easing.progress(15F, 10F, 10F), 1e-5F);
    }

    @Test
    void smoothedFollowsIndependentlyOfFrameRate() {
        Smoothed a = new Smoothed(), b = new Smoothed();
        a.snap(0F);
        b.snap(0F);
        a.update(10F, 2F, 0.3F);
        b.update(10F, 1F, 0.3F);
        b.update(10F, 1F, 0.3F);
        assertEquals(a.get(), b.get(), 1e-4F, "two frames of 1 tick = one frame of 2 ticks");
    }

    @Test
    void paletteBlendsAndFades() {
        assertEquals(NeonPalette.VIOLET, NeonPalette.lerp(NeonPalette.VIOLET, NeonPalette.RED, 0F));
        assertEquals(NeonPalette.RED, NeonPalette.lerp(NeonPalette.VIOLET, NeonPalette.RED, 1F));
        assertEquals(0x80, NeonPalette.fade(0xFF123456, 0.5F) >>> 24 & 0xFF, 1);
        assertEquals(0x123456, NeonPalette.fade(0xFF123456, 0.5F) & 0xFFFFFF);
    }
}
