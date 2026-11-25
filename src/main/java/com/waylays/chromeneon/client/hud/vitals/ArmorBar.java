package com.waylays.chromeneon.client.hud.vitals;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Armor: thin slanted plates in steel white, one per two points, distinct from the health chevrons. Plates gained
 * flash cyan and settle; plates lost burn orange and fade.
 */
final class ArmorBar {
    static final float LABEL_W = 16F, W = 4F, H = 5F, SKEW = 2F, STRIDE = 6F;

    private ArmorBar() {}

    static int plates(VitalsTracker t) {
        return Math.clamp((int) Math.ceil(Math.max(20, t.armor) / 2F), 10, 15);
    }

    static float width(VitalsTracker t) {
        return LABEL_W + plates(t) * STRIDE + SKEW + 22F;
    }

    static void draw(GuiGraphics g, HudFrame f, VitalsTracker t, float x, float y, float boot) {
        HudText.draw(g, HudText.micro("ARM"), x, y, f.c(NeonPalette.LAVENDER, 0.8F));
        int n = plates(t);
        float age = f.now - t.armorAt;
        int lit = boot >= 1F ? n : (int) Math.floor(n * boot);
        for (int i = 0; i < n; i++) {
            float px = x + LABEL_W + i * STRIDE;
            float points = Math.clamp(t.armor - i * 2F, 0F, 2F) / 2F;
            HudDraw.parallelogram(g, px, y, W, H, SKEW, f.c(NeonPalette.STEEL, 0.13F));
            if (i >= lit) continue;
            float lo = i * 2F, hi = lo + 2F;
            boolean gained = t.armor > t.armorFrom && hi > t.armorFrom && lo < t.armor && age < 12F;
            boolean lost = t.armor < t.armorFrom && hi > t.armor && lo < t.armorFrom && age < 16F;
            if (points > 0F) {
                float lift = gained ? (1F - Easing.outCubic(age / 12F)) * 2F : 0F;
                int color = gained ? NeonPalette.lerp(NeonPalette.CYAN, NeonPalette.STEEL, age / 12F) : NeonPalette.STEEL;
                HudDraw.parallelogram(g, px, y - lift, W * points, H, SKEW * points, f.c(color));
            }
            if (lost && points < 1F) {
                HudDraw.parallelogram(g, px, y, W, H, SKEW, f.c(NeonPalette.ORANGE, 1F - age / 16F));
            }
        }
        String value = t.armor + (t.toughness >= 1F ? "  T" + Math.round(t.toughness) : "");
        HudText.draw(g, HudText.micro(value), x + LABEL_W + n * STRIDE + SKEW + 4F, y, f.c(NeonPalette.STEEL, 0.9F));
    }
}
