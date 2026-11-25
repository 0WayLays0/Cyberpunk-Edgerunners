package com.waylays.chromeneon.client.hud.vitals;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Armor: a comb of thin slanted steel blades, one per armor point, finer than the health segments. Blades gained
 * flash cyan and rise into place; blades lost burn orange and fade.
 */
final class ArmorBar {
    static final float LABEL_W = 22F, W = 1.2F, H = 4.5F, SKEW = 1.6F, STRIDE = 2.4F;

    private ArmorBar() {}

    static int blades(VitalsTracker t) {
        return Math.clamp(Math.max(20, t.armor), 20, 30);
    }

    static float width(VitalsTracker t) {
        return LABEL_W + blades(t) * STRIDE + SKEW + 22F;
    }

    static void draw(GuiGraphics g, HudFrame f, VitalsTracker t, float x, float y, float boot) {
        float px = f.px;
        HudText.draw(g, HudText.label("ARMOR"), x, y + 0.5F, f.c(NeonPalette.LAVENDER, 0.85F * boot), px);
        int n = blades(t);
        float age = f.now - t.armorAt;
        int lit = boot >= 1F ? n : (int) Math.floor(n * boot);
        float bx = x + LABEL_W;
        for (int i = 0; i < n; i++) {
            float sx = bx + i * STRIDE;
            boolean on = i < t.armor;
            boolean gained = t.armor > t.armorFrom && i >= t.armorFrom && i < t.armor && age < 12F;
            boolean lost = t.armor < t.armorFrom && i >= t.armor && i < t.armorFrom && age < 16F;
            if (on && i < lit) {
                float lift = gained ? (1F - Easing.outCubic(age / 12F)) * 2F : 0F;
                int color = gained ? NeonPalette.lerp(NeonPalette.CYAN, NeonPalette.STEEL, age / 12F) : NeonPalette.STEEL;
                HudDraw.parallelogram(g, sx, y - lift, W, H, SKEW, f.c(color));
            } else {
                HudDraw.parallelogram(g, sx, y + H - px * 2, W, px * 2, 0F, f.c(NeonPalette.STEEL, 0.3F));
            }
            if (lost) HudDraw.parallelogram(g, sx, y, W, H, SKEW, f.c(NeonPalette.ORANGE, 1F - age / 16F));
        }
        String value = String.valueOf(t.armor) + (t.toughness >= 1F ? "  T" + Math.round(t.toughness) : "");
        HudText.draw(g, HudText.data(value), bx + n * STRIDE + SKEW + 4F, y, f.c(NeonPalette.STEEL, 0.9F * boot), px);
    }
}
