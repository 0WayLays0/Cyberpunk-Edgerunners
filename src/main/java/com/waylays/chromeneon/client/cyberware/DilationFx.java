package com.waylays.chromeneon.client.cyberware;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The look of time dilation from inside: the screen's edges bleed violet and cyan, fine streaks drift slowly from
 * the edges, and the remaining time counts down under the crosshair. Fades in and out with the effect.
 */
public final class DilationFx {
    private DilationFx() {}

    public static void render(GuiGraphics g, HudFrame f) {
        var accelerator = ClientCyberware.accelerator().orElse(null);
        if (accelerator == null) return;
        var state = accelerator.state();
        long now = ClientCyberware.now();
        boolean running = state.active && now < state.activeUntil;
        float since = now - state.changedAt;
        float k = running ? Easing.outCubic(since / 6F) : 1F - Easing.clamp(since / 8F);
        if (k <= 0.01F) return;
        k *= Math.max(0.35F, f.fx);
        float depth = Math.min(f.width, f.height) * 0.22F;
        int violet = NeonPalette.alpha(NeonPalette.VIOLET, 0.35F * k), cyan = NeonPalette.alpha(NeonPalette.CYAN, 0.18F * k);
        HudDraw.hGradient(g, 0, 0, depth, f.height, violet, violet & 0x00FFFFFF);
        HudDraw.hGradient(g, f.width - depth, 0, f.width, f.height, cyan & 0x00FFFFFF, cyan);
        HudDraw.vGradient(g, 0, 0, f.width, depth * 0.6F, violet, violet & 0x00FFFFFF);
        HudDraw.vGradient(g, 0, f.height - depth * 0.6F, f.width, f.height, cyan & 0x00FFFFFF, cyan);
        for (int i = 0; i < 14; i++) {
            float y = (i * 0.0713F + 0.03F) % 1F * f.height;
            float drift = ((f.now * 0.6F + i * 37F) % 60F) / 60F;
            float len = 20F + (i % 4) * 14F;
            float x = (i % 2 == 0) ? drift * depth : f.width - drift * depth - len;
            HudDraw.rect(g, x, y, x + len, y + f.screenPx, NeonPalette.alpha(i % 2 == 0 ? NeonPalette.VIOLET_HOT : NeonPalette.CYAN, 0.35F * k * (1F - drift)));
        }
        if (running) {
            float left = (state.activeUntil - now) / 20F;
            Component t = HudText.value(String.format(java.util.Locale.ROOT, "%.1f", left));
            Component l = HudText.label("CHRONOS " + accelerator.implant().model() + " // TIME DILATION");
            float cy = f.height / 2F + 22F;
            HudText.draw(g, t, (f.width - HudText.width(t)) / 2F, cy, NeonPalette.alpha(NeonPalette.WHITE, k), f.screenPx);
            HudText.draw(g, l, (f.width - HudText.width(l)) / 2F, cy + HudText.Kind.VALUE.cap() + 3F, NeonPalette.alpha(NeonPalette.VIOLET_HOT, k), f.screenPx);
        }
    }
}
