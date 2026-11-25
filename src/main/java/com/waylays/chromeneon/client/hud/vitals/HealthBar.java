package com.waylays.chromeneon.client.hud.vitals;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Health: a tab with the value, then a run of chevrons pointing forward. Damage leaves a white chunk that cools to
 * orange and drains away; healing sweeps cyan; below 30% the bar turns red and beats. Absorption appends hollow
 * chevrons. Health above 40 points is spread over 20 chevrons.
 */
final class HealthBar {
    static final float SEG_W = 4F, SEG_H = 9F, DEPTH = 3F, STRIDE = 6F, TAB_H = 11F;

    private HealthBar() {}

    static int segments(float max) {
        return Math.clamp((int) Math.ceil(max / 2F), 5, 20);
    }

    static float tabWidth(VitalsTracker t) {
        return Math.max(22F, HudText.width(HudText.hud(value(t))) + 12F);
    }

    static float width(VitalsTracker t, HudFrame f) {
        float per = t.maxHealth / segments(t.maxHealth);
        int absorb = (int) Math.ceil(t.absorption / per);
        return tabWidth(t) + 4F + segments(t.maxHealth) * STRIDE + DEPTH + absorb * STRIDE + (t.critical ? 52F : 8F);
    }

    private static String value(VitalsTracker t) {
        return String.valueOf((int) Math.ceil(t.health));
    }

    static void draw(GuiGraphics g, HudFrame f, VitalsTracker t, float x, float y, float boot) {
        int n = segments(t.maxHealth);
        float per = t.maxHealth / n;
        float shown = t.shown.update(t.health, f.delta, 0.45F);
        float tabW = tabWidth(t);
        float bx = x + tabW + 4F;
        float barEnd = bx + n * STRIDE + DEPTH;
        float pulse = Easing.heartbeat(f.now, 18F);
        int lit = boot >= 1F ? n : (int) Math.floor(n * boot);

        if (!f.minimal) HudDraw.rect(g, bx - 2, y - 1, barEnd + 2, y + TAB_H + 1, f.accent(0.07F));
        for (int i = 0; i < n; i++) {
            float sx = bx + i * STRIDE, sy = y + 1;
            int empty = t.critical ? f.c(NeonPalette.RED_DEEP, 0.9F) : f.c(NeonPalette.VIOLET_DEEP, 0.8F);
            HudDraw.chevron(g, sx, sy, SEG_W, SEG_H, DEPTH, empty);
            if (i >= lit) continue;
            float fill = Math.clamp((shown - i * per) / per, 0F, 1F);
            float trail = Math.clamp((t.trail - i * per) / per, 0F, 1F);
            if (trail > fill + 0.01F) {
                float age = (f.now - t.damageAt) / 10F;
                HudDraw.chevron(g, sx, sy, SEG_W * trail, SEG_H, DEPTH,
                        f.c(NeonPalette.lerp(NeonPalette.WHITE, NeonPalette.ORANGE, Easing.clamp(age))));
            }
            if (fill > 0.01F) {
                HudDraw.chevron(g, sx, sy, Math.max(1F, SEG_W * fill), SEG_H, DEPTH, f.c(segmentColor(f, t, i, per, pulse)));
            }
        }
        // Absorption: hollow white chevrons after the bar
        int absorb = (int) Math.ceil(t.absorption / per);
        for (int i = 0; i < absorb && boot >= 1F; i++) {
            float sx = barEnd + 2 + i * STRIDE;
            HudDraw.chevron(g, sx, y + 1, SEG_W, SEG_H, DEPTH, f.c(NeonPalette.WHITE, 0.85F));
            HudDraw.chevron(g, sx + 1.2F, y + 3, SEG_W - 2F, SEG_H - 4F, DEPTH - 1F, f.c(NeonPalette.VOID, 0.9F));
        }

        // The value tab, sliding in during the boot
        float tabX = x - (1F - Easing.outCubic(boot)) * 12F;
        int tabColor = t.critical
                ? f.c(NeonPalette.lerp(NeonPalette.ORANGE, NeonPalette.RED, pulse))
                : f.accent();
        HudDraw.cutPanel(g, tabX, y, tabW, TAB_H, 0, 0, 4, 3, tabColor);
        Component value = HudText.hud(value(t));
        HudText.draw(g, value, tabX + (tabW - 4F - HudText.width(value)) / 2F + 1F, y + 2, f.c(NeonPalette.VOID));

        if (!f.minimal) {
            float uy = y + TAB_H + 1;
            HudDraw.hLine(g, bx, barEnd, uy, f.accent(0.45F));
            HudDraw.line(g, barEnd, uy + 0.5F, barEnd + 3, uy + 3.5F, 1F, f.accent(0.45F));
            HudDraw.hLine(g, barEnd + 3, barEnd + 9, uy + 3, f.accent(0.45F));
        }
        if (t.critical) {
            Component tag = HudText.micro(Component.translatable("chromeneon.hud.critical"));
            float cx = barEnd + 4 + absorb * STRIDE, w = HudText.width(tag) + 8F;
            float alpha = Easing.blink(f.now, 12F) ? 1F : 0.55F;
            HudDraw.parallelogram(g, cx, y + 2, w, 7, 2, f.c(NeonPalette.RED, alpha));
            HudText.draw(g, tag, cx + 4F, y + 3, f.c(NeonPalette.VOID, alpha));
        }
    }

    private static int segmentColor(HudFrame f, VitalsTracker t, int i, float per, float pulse) {
        int base = t.critical ? NeonPalette.lerp(NeonPalette.ORANGE, NeonPalette.RED, 0.4F + 0.6F * pulse)
                : NeonPalette.lerp(NeonPalette.VIOLET, NeonPalette.ORANGE, f.alert * 0.6F);
        float healAge = (f.now - t.healAt) / 14F;
        float segStart = i * per, segEnd = segStart + per;
        if (healAge < 1F && segEnd > t.healFrom && segStart < t.healTo) {
            return NeonPalette.lerp(NeonPalette.CYAN, base, Easing.outCubic(healAge));
        }
        return base;
    }

    /** The state word of the label row above the bar. */
    static Component state(HudFrame f, VitalsTracker t) {
        if (t.health <= 0F) return Component.translatable("chromeneon.hud.vitals.flatline");
        if (t.critical) return Component.translatable("chromeneon.hud.vitals.critical");
        if (f.now - t.damageAt < 40F) return Component.translatable("chromeneon.hud.vitals.damaged");
        return Component.translatable("chromeneon.hud.vitals.nominal");
    }
}
