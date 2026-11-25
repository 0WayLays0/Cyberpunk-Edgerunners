package com.waylays.chromeneon.client.hud.fx;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudTime;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The HUD powering on when the player joins a world: a scan line runs down the screen and each group comes up in
 * turn ({@link HudFrame#boot}). Groups read their progress; nothing else waits for it.
 */
public final class BootSequence {
    private static final float SWEEP = 22F, ELEMENT = 14F;
    private float start = -1000F;

    public void start() {
        start = ClientConfig.BOOT_SEQUENCE.get() ? HudTime.ticks() : -1000F;
    }

    /** 0..1 for an element starting {@code delay} ticks after the boot. */
    public float progress(float now, int delay) {
        return Easing.clamp((now - start - delay) / ELEMENT);
    }

    public void render(GuiGraphics g, HudFrame f) {
        float t = (f.now - start) / SWEEP;
        if (t < 0F || t > 1F) return;
        float y = f.height * Easing.inOutCubic(t);
        float fade = 1F - Easing.smoothstep((t - 0.75F) / 0.25F);
        HudDraw.vGradient(g, 0, y - 28, f.width, y, NeonPalette.alpha(NeonPalette.VIOLET, 0F),
                NeonPalette.alpha(NeonPalette.VIOLET, 0.10F * fade));
        HudDraw.rect(g, 0, y, f.width, y + f.screenPx, NeonPalette.alpha(NeonPalette.CYAN, 0.7F * fade));
        var label = HudText.label("CHROME NÉON // SYS.INIT " + Math.round(t * 100) + "%");
        HudText.draw(g, label, (f.width - HudText.width(label)) / 2F, f.height / 2F + 14, NeonPalette.alpha(NeonPalette.CYAN, fade), f.screenPx);
    }
}
