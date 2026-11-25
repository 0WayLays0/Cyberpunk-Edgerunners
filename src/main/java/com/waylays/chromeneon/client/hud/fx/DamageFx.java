package com.waylays.chromeneon.client.hud.fx;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudTime;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Screen feedback for damage and danger, always on the screen's edges so the centre stays readable: a red edge
 * flash scaled to the blow, a wedge around the crosshair pointing at the source, and the low-health heartbeat.
 */
public final class DamageFx {
    private static final float FLASH = 14F, DIRECTION = 34F;

    /** {@code strength} is the share of max health lost; {@code sourceYaw} is NaN when the damage has no direction. */
    private record Hit(float time, float strength, float sourceYaw) {}

    private final Deque<Hit> hits = new ArrayDeque<>();

    public void hit(float strength, float sourceYaw) {
        hits.addFirst(new Hit(HudTime.ticks(), Math.clamp(strength, 0.05F, 1F), sourceYaw));
        while (hits.size() > 6) hits.removeLast();
    }

    public void clear() {
        hits.clear();
    }

    public void render(GuiGraphics g, HudFrame f, LocalPlayer player, float criticalSeverity) {
        if (f.fx <= 0F) return;
        float flash = 0F;
        for (Hit h : hits) {
            float t = (f.now - h.time) / FLASH;
            if (t >= 0F && t < 1F) flash = Math.max(flash, h.strength * (1F - Easing.outCubic(t)));
        }
        float beat = 0F;
        if (criticalSeverity > 0F) {
            float period = 22F - 10F * criticalSeverity;
            beat = (0.22F + 0.4F * Easing.heartbeat(f.now, period)) * criticalSeverity;
        }
        float edge = Math.min(1F, Math.max(flash * 1.6F, beat)) * f.fx;
        if (edge > 0.01F) vignette(g, f, edge, flash > beat ? NeonPalette.RED : NeonPalette.lerp(NeonPalette.RED, NeonPalette.ORANGE, 0.25F));
        for (Hit h : hits) {
            float t = (f.now - h.time) / DIRECTION;
            if (Float.isNaN(h.sourceYaw) || t < 0F || t >= 1F) continue;
            float rel = (float) Math.toRadians(h.sourceYaw - player.getViewYRot(1F));
            wedge(g, f, rel, (1F - Easing.inCubic(t)) * Math.min(1F, 0.5F + h.strength * 2F) * f.fx, t);
        }
    }

    /** Gradients from the four edges: deepest in the corners, nothing in the middle of the screen. */
    private static void vignette(GuiGraphics g, HudFrame f, float strength, int color) {
        float depth = Math.min(f.width, f.height) * (0.10F + 0.12F * strength);
        int solid = NeonPalette.alpha(color, 0.55F * strength), clear = NeonPalette.alpha(color, 0F);
        HudDraw.vGradient(g, 0, 0, f.width, depth, solid, clear);
        HudDraw.vGradient(g, 0, f.height - depth, f.width, f.height, clear, solid);
        HudDraw.hGradient(g, 0, 0, depth, f.height, solid, clear);
        HudDraw.hGradient(g, f.width - depth, 0, f.width, f.height, clear, solid);
    }

    /** An arc segment with a chevron tip on a ring around the crosshair, toward the damage source. */
    private static void wedge(GuiGraphics g, HudFrame f, float angle, float alpha, float t) {
        float cx = f.width / 2F, cy = f.height / 2F, r = 30F + 6F * Easing.outCubic(t);
        float span = 0.32F;
        int color = NeonPalette.alpha(NeonPalette.lerp(NeonPalette.WHITE, NeonPalette.RED, Math.min(1F, t * 4F)), alpha);
        HudDraw.arc(g, cx, cy, r, 2.5F, angle - span, angle + span, 8, color);
        HudDraw.arc(g, cx, cy, r + 4F, 1F, angle - span * 0.6F, angle + span * 0.6F, 6, NeonPalette.fade(color, 0.6F));
        float sx = (float) Math.sin(angle), sy = (float) -Math.cos(angle);
        float tipR = r + 9F, baseR = r + 5.5F, px = -sy, py = sx;
        HudDraw.triangle(g, cx + sx * tipR, cy + sy * tipR, cx + sx * baseR + px * 3F, cy + sy * baseR + py * 3F,
                cx + sx * baseR - px * 3F, cy + sy * baseR - py * 3F, color);
    }
}
