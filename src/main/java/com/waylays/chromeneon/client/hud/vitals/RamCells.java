package com.waylays.chromeneon.client.hud.vitals;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.network.SyncCyberStatePayload;
import com.waylays.chromeneon.common.player.SyncedCyberState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * RAM: slanted cyan cells, each outlined and lit with a glow when available. The cell being recovered fills from
 * left to right, spent cells flash white and go dark, new cells drop into place, and a request for more RAM than
 * available outlines the missing cells in red while the row shakes.
 */
final class RamCells {
    static final float LABEL_W = 22F, W = 5.5F, H = 4.5F, SKEW = 1.8F, STRIDE = 7.4F;

    private RamCells() {}

    static boolean active() {
        SyncCyberStatePayload s = SyncedCyberState.current();
        return s != null && s.stats().ramCapacity() > 0;
    }

    static float width(HudFrame f, RamTracker r) {
        SyncCyberStatePayload s = SyncedCyberState.current();
        int cap = s == null ? 0 : s.stats().ramCapacity();
        float w = LABEL_W + cap * STRIDE + 26F;
        if (f.now - r.insufficientAt < RamTracker.INSUFFICIENT_TICKS) {
            w += HudText.width(HudText.label(Component.translatable("chromeneon.hud.ram.insufficient"))) + 6F;
        }
        return w;
    }

    static void draw(GuiGraphics g, HudFrame f, RamTracker r, float x, float y, float boot) {
        SyncCyberStatePayload state = SyncedCyberState.current();
        if (state == null) return;
        float px = f.px;
        int cap = Math.min(64, state.stats().ramCapacity()), ram = state.stats().ram();
        float regen = SyncedCyberState.regenFraction();
        float lack = f.now - r.insufficientAt;
        boolean insufficient = lack < RamTracker.INSUFFICIENT_TICKS;
        float shake = insufficient ? (float) Math.sin(f.now * 2.6F) * (1F - lack / RamTracker.INSUFFICIENT_TICKS) * 1.5F : 0F;

        HudText.draw(g, HudText.label("RAM"), x, y + 0.5F, f.c(NeonPalette.CYAN_SOFT, 0.9F * boot), px);
        int lit = boot >= 1F ? cap : (int) Math.floor(cap * boot);
        float bx = x + LABEL_W;
        if (ram > 0 && boot >= 1F) {
            HudDraw.fadeRight(g, bx - 1, y - 1.5F, bx + ram * STRIDE + 6, y + H + 1.5F, f.c(NeonPalette.CYAN, 0.12F));
        }
        for (int i = 0; i < cap; i++) {
            float cx = bx + i * STRIDE + shake, cy = y;
            float pop = Easing.progress(f.now, r.addedAt[i], 10F);
            if (pop < 1F) cy -= (1F - Easing.outBack(pop)) * 4F;
            boolean missing = insufficient && i >= ram && i < r.insufficientCost;
            if (i < ram && i < lit) {
                HudDraw.parallelogram(g, cx, cy, W, H, SKEW, f.c(NeonPalette.lerp(NeonPalette.CYAN, NeonPalette.WHITE, 0.45F)),
                        f.c(NeonPalette.CYAN));
            } else {
                int frame = missing && Easing.blink(f.now, 6F) ? f.c(NeonPalette.RED) : f.c(NeonPalette.CYAN, 0.45F);
                HudDraw.parallelogramFrame(g, cx, cy, W, H, SKEW, missing ? px * 2 : px, frame);
                if (i == ram && regen > 0F && i < lit) {
                    HudDraw.parallelogram(g, cx + px, cy + px, (W - 2 * px) * regen, H - 2 * px, SKEW * regen,
                            f.c(NeonPalette.CYAN, 0.5F));
                }
            }
            float flash = Easing.progress(f.now, r.spentAt[i], 8F);
            if (flash < 1F) HudDraw.parallelogram(g, cx, cy, W, H, SKEW, f.c(NeonPalette.WHITE, 1F - flash));
        }
        float tx = bx + cap * STRIDE + 3F + shake;
        HudText.draw(g, HudText.data(ram + "/" + cap), tx, y, f.c(NeonPalette.CYAN_SOFT, boot), px);
        if (insufficient) {
            Component warn = HudText.label(Component.translatable("chromeneon.hud.ram.insufficient"));
            HudText.draw(g, warn, tx + 20F, y + 0.5F, f.c(NeonPalette.RED, Easing.blink(f.now, 6F) ? 1F : 0.6F), px);
        }
    }
}
