package com.waylays.chromeneon.client.hud.vitals;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Health: the value in large digits, then a run of fine slanted segments (one per health point) with a sheen,
 * capped by a bright chevron where the fill ends, a hairline rule under them that breaks upward at its end, and the
 * exact reading under the rule. Damage leaves a white chunk that cools to orange and drains; healing sweeps cyan;
 * critical health turns the bar red with a heartbeat glow and a slanted CRITICAL tag.
 */
final class HealthBar {
    static final float SEG_W = 2.6F, SEG_H = 7F, SKEW = 2.4F, STRIDE = 3.6F, TOP = 2F;

    private HealthBar() {}

    static int segments(float max) {
        return Math.clamp((int) Math.ceil(max), 10, 40);
    }

    static float barX(VitalsTracker t) {
        return HudText.width(HudText.value(value(t, 20))) + 6F;
    }

    static float barEnd(VitalsTracker t) {
        return barX(t) + segments(t.maxHealth) * STRIDE + SKEW;
    }

    static float width(VitalsTracker t) {
        float per = t.maxHealth / segments(t.maxHealth);
        return barEnd(t) + (int) Math.ceil(t.absorption / per) * STRIDE + (t.critical ? 46F : 12F);
    }

    private static String value(VitalsTracker t, float fallback) {
        return String.valueOf((int) Math.ceil(t == null ? fallback : t.health));
    }

    static void draw(GuiGraphics g, HudFrame f, VitalsTracker t, float x, float y, float boot) {
        int n = segments(t.maxHealth);
        float per = t.maxHealth / n;
        float shown = t.shown.update(t.health, f.delta, 0.45F);
        float px = f.px;
        float bx = x + barX(t), by = y + TOP;
        float end = bx + n * STRIDE + SKEW;
        float beat = Easing.heartbeat(f.now, 18F);
        int lit = boot >= 1F ? n : (int) Math.floor(n * boot);

        // The number
        int numberColor = t.critical ? f.c(NeonPalette.lerp(NeonPalette.ORANGE, NeonPalette.RED, beat)) : f.c(NeonPalette.WHITE);
        Component number = HudText.value(value(t, 0));
        HudText.drawGlow(g, number, x, y, NeonPalette.fade(numberColor, Easing.outCubic(boot)), px);

        // Glow under the filled part
        float fillEnd = bx + Math.min(n, shown / per) * STRIDE;
        int glow = t.critical ? NeonPalette.RED : NeonPalette.lerp(NeonPalette.VIOLET, NeonPalette.ORANGE, f.alert * 0.6F);
        HudDraw.fadeRight(g, bx - 1, by - 1.5F, Math.max(bx, fillEnd) + 6, by + SEG_H + 1.5F,
                f.c(glow, (t.critical ? 0.18F + 0.2F * beat : 0.12F) * boot));

        for (int i = 0; i < n; i++) {
            float sx = bx + i * STRIDE;
            if (i >= lit) continue;
            float fill = Math.clamp((shown - i * per) / per, 0F, 1F);
            float trail = Math.clamp((t.trail - i * per) / per, 0F, 1F);
            if (fill < 0.99F) {
                HudDraw.parallelogramFrame(g, sx, by, SEG_W, SEG_H, SKEW, px,
                        f.c(t.critical ? NeonPalette.RED : NeonPalette.VIOLET, 0.35F));
            }
            if (trail > fill + 0.01F) {
                float age = (f.now - t.damageAt) / 10F;
                HudDraw.parallelogram(g, sx, by, SEG_W * trail, SEG_H, SKEW * trail,
                        f.c(NeonPalette.lerp(NeonPalette.WHITE, NeonPalette.ORANGE, Easing.clamp(age))));
            }
            if (fill > 0.01F) {
                int base = segmentColor(f, t, i, per, beat);
                HudDraw.parallelogram(g, sx, by, SEG_W * fill, SEG_H, SKEW * fill,
                        f.c(NeonPalette.lerp(base, NeonPalette.WHITE, 0.35F)), f.c(base));
            }
        }
        // The chevron cap where the fill ends
        if (shown > 0F && boot >= 1F) {
            float cx = Math.min(fillEnd, end - SKEW) + 1.2F;
            int cap = t.critical ? NeonPalette.RED : NeonPalette.WHITE;
            HudDraw.stroke(g, new float[] {cx, by - 1, cx + 3, by + SEG_H / 2F, cx, by + SEG_H + 1}, px * 2, f.c(cap, 0.9F));
        }
        // Absorption: open white segments after the bar
        int absorb = (int) Math.ceil(t.absorption / per);
        for (int i = 0; i < absorb && boot >= 1F; i++) {
            HudDraw.parallelogramFrame(g, end + 3 + i * STRIDE, by, SEG_W, SEG_H, SKEW, px * 1.5F, f.c(NeonPalette.WHITE, 0.9F));
        }

        // The rule: from under the number, along the bar, breaking upward at its end
        float ry = by + SEG_H + 3F;
        float reach = x + (end + 8 - x) * Easing.outCubic(boot);
        HudDraw.rect(g, x, ry, reach, ry + px, f.accent(0.75F));
        if (boot >= 1F) {
            HudDraw.line(g, end + 8, ry + px / 2, end + 12, ry - 4, px, f.accent(0.75F));
            HudDraw.rect(g, x, ry - px, x + 6, ry + px * 2, f.accent());
        }
        if (t.critical) {
            Component tag = HudText.label(Component.translatable("chromeneon.hud.critical"));
            float tx = end + 4 + absorb * STRIDE + 4, w = HudText.width(tag) + 7F;
            float a = Easing.blink(f.now, 12F) ? 1F : 0.55F;
            HudDraw.parallelogram(g, tx, by, w, SEG_H, 2F, f.c(NeonPalette.RED, a));
            HudText.drawFlat(g, tag, tx + 3.5F, by + (SEG_H - HudText.Kind.LABEL.cap()) / 2F, f.c(NeonPalette.VOID, a));
        }
    }

    /** Where the label row under the rule starts. */
    static float labelY(float y) {
        return y + TOP + SEG_H + 5.5F;
    }

    private static int segmentColor(HudFrame f, VitalsTracker t, int i, float per, float beat) {
        int base = t.critical ? NeonPalette.lerp(NeonPalette.ORANGE, NeonPalette.RED, 0.4F + 0.6F * beat)
                : NeonPalette.lerp(NeonPalette.VIOLET, NeonPalette.ORANGE, f.alert * 0.6F);
        float healAge = (f.now - t.healAt) / 14F;
        float segStart = i * per, segEnd = segStart + per;
        if (healAge < 1F && segEnd > t.healFrom && segStart < t.healTo) {
            return NeonPalette.lerp(NeonPalette.CYAN, base, Easing.outCubic(healAge));
        }
        return base;
    }

    /** The state word of the label row. */
    static Component state(HudFrame f, VitalsTracker t) {
        if (t.health <= 0F) return Component.translatable("chromeneon.hud.vitals.flatline");
        if (t.critical) return Component.translatable("chromeneon.hud.vitals.critical");
        if (f.now - t.damageAt < 40F) return Component.translatable("chromeneon.hud.vitals.damaged");
        return Component.translatable("chromeneon.hud.vitals.nominal");
    }
}
