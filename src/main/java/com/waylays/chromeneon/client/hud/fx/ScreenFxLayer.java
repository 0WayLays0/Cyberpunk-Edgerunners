package com.waylays.chromeneon.client.hud.fx;

import com.waylays.chromeneon.client.debug.HudPerf;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudVisibility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/** The full-screen layer, drawn under every other GUI layer: screen tearing, damage and danger, boot sweep. */
public final class ScreenFxLayer implements LayeredDraw.Layer {
    private static final ScreenGlitch GLITCH = new ScreenGlitch();
    private final ScreenGlitch glitch = GLITCH;

    /** The screen copy used by the tearing, for the automated test. */
    public static ScreenGlitch glitch() {
        return GLITCH;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        HudFrame f = HudFrame.current();
        Minecraft mc = Minecraft.getInstance();
        if (f == null || !HudVisibility.hud() || mc.player == null) return;
        long start = System.nanoTime();
        float strength = ChromeHud.glitch().strength(f.now) * f.fx;
        if (strength > 0F) glitch.render(g, f, ChromeHud.glitch().bands(f.now), strength);
        com.waylays.chromeneon.client.cyberware.DilationFx.render(g, f);
        ChromeHud.damage().render(g, f, mc.player, ChromeHud.vitals().criticalSeverity());
        ChromeHud.boot().render(g, f);
        HudPerf.record("screen_fx", System.nanoTime() - start);
    }
}
