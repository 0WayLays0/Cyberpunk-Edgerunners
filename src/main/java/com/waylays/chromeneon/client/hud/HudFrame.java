package com.waylays.chromeneon.client.hud;

import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

/**
 * What every HUD element needs to know about the current frame, read once per frame: time, screen size, the
 * player's settings and the danger level. Elements never read the config themselves while drawing.
 */
public final class HudFrame {
    private static HudFrame current;
    private static float lastNow = Float.NaN;

    /** HUD time in ticks, with the partial tick. */
    public final float now;
    /** Ticks since the previous frame (for frame-rate independent smoothing). */
    public final float delta;
    public final int width, height;
    public final float scale, opacity, fx;
    public final boolean minimal, glitch;
    /** 0 when healthy, toward 1 as health becomes critical: the interface intensifies with it. */
    public final float alert;
    public final long millis;

    private HudFrame(float now, float delta, int width, int height, float alert) {
        this.now = now;
        this.delta = delta;
        this.width = width;
        this.height = height;
        this.scale = ClientConfig.HUD_SCALE.get().floatValue();
        this.opacity = ClientConfig.HUD_OPACITY.get().floatValue();
        this.fx = ClientConfig.FX_INTENSITY.get().floatValue();
        this.minimal = ClientConfig.MINIMAL.get();
        this.glitch = ClientConfig.GLITCH_EFFECTS.get();
        this.alert = alert;
        this.millis = Util.getMillis();
    }

    /** Called at the start of every rendered frame. */
    public static void begin(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        float now = HudTime.now(partialTick);
        float delta = Float.isNaN(lastNow) ? 0F : Math.clamp(now - lastNow, 0F, 5F);
        lastNow = now;
        HudBounds.newFrame();
        current = new HudFrame(now, delta, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                ChromeHud.vitals().alert(delta));
    }

    /** The frame being drawn; null before the first frame. */
    public static HudFrame current() {
        return current;
    }

    /** The colour at the HUD's opacity. */
    public int c(int argb) {
        return NeonPalette.fade(argb, opacity);
    }

    /** The colour at the HUD's opacity times {@code alpha}. */
    public int c(int argb, float alpha) {
        return NeonPalette.fade(argb, opacity * alpha);
    }

    /** The system colour of frames and labels: violet, drifting to red as danger rises. */
    public int accent() {
        return c(NeonPalette.lerp(NeonPalette.VIOLET, NeonPalette.RED, alert * 0.85F));
    }

    public int accent(float alpha) {
        return NeonPalette.fade(accent(), alpha);
    }

    /** Power-on progress of an element whose boot starts {@code delay} ticks into the sequence. */
    public float boot(int delay) {
        return ChromeHud.boot().progress(now, delay);
    }
}
