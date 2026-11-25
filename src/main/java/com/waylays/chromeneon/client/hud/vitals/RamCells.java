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
 * RAM as physical cells: little cyan chips with a cut corner. Spent cells go dark with a white flash, the cell
 * being recovered fills from the bottom, cells added pop in, and a request for more RAM than available makes the
 * missing cells flash red and the row shake.
 */
final class RamCells {
    static final float LABEL_W = 16F, W = 5F, H = 7F, STRIDE = 7F;

    private RamCells() {}

    static boolean active() {
        SyncCyberStatePayload s = SyncedCyberState.current();
        return s != null && s.stats().ramCapacity() > 0;
    }

    static float width(HudFrame f, RamTracker r) {
        SyncCyberStatePayload s = SyncedCyberState.current();
        int cap = s == null ? 0 : s.stats().ramCapacity();
        float w = LABEL_W + cap * STRIDE + 28F;
        if (f.now - r.insufficientAt < RamTracker.INSUFFICIENT_TICKS) {
            w += HudText.width(HudText.micro(Component.translatable("chromeneon.hud.ram.insufficient"))) + 4F;
        }
        return w;
    }

    static void draw(GuiGraphics g, HudFrame f, RamTracker r, float x, float y, float boot) {
        SyncCyberStatePayload state = SyncedCyberState.current();
        if (state == null) return;
        int cap = Math.min(64, state.stats().ramCapacity()), ram = state.stats().ram();
        float regen = SyncedCyberState.regenFraction();
        float lack = f.now - r.insufficientAt;
        boolean insufficient = lack < RamTracker.INSUFFICIENT_TICKS;
        float shake = insufficient ? (float) Math.sin(f.now * 2.6F) * (1F - lack / RamTracker.INSUFFICIENT_TICKS) * 1.5F : 0F;

        HudText.draw(g, HudText.micro("RAM"), x, y + 1, f.c(NeonPalette.CYAN_SOFT, 0.85F));
        int lit = boot >= 1F ? cap : (int) Math.floor(cap * boot);
        for (int i = 0; i < cap; i++) {
            float cx = x + LABEL_W + i * STRIDE + shake, cy = y;
            float pop = Easing.progress(f.now, r.addedAt[i], 10F);
            if (pop < 1F) {
                cy += (1F - Easing.outBack(pop)) * 4F;
            }
            boolean available = i < ram && i < lit;
            if (available) {
                HudDraw.cutPanel(g, cx, cy, W, H, 0, 2, 0, 0, f.c(NeonPalette.CYAN));
                HudDraw.rect(g, cx + 1, cy + H - 2, cx + W - 1, cy + H - 1, f.c(NeonPalette.CYAN_DEEP));
                HudDraw.rect(g, cx, cy, cx + 1, cy + 3, f.c(NeonPalette.WHITE, 0.7F));
            } else {
                HudDraw.cutPanel(g, cx, cy, W, H, 0, 2, 0, 0, f.c(NeonPalette.VOID, 0.75F));
                boolean missing = insufficient && i >= ram && i < r.insufficientCost;
                int frame = missing && Easing.blink(f.now, 6F) ? f.c(NeonPalette.RED) : f.c(NeonPalette.CYAN, 0.35F);
                HudDraw.cutFrame(g, cx, cy, W, H, 0, 2, 0, 0, frame);
                if (i == ram && regen > 0F && i < lit) {
                    float fh = (H - 2F) * regen;
                    HudDraw.rect(g, cx + 1, cy + H - 1 - fh, cx + W - 1, cy + H - 1, f.c(NeonPalette.CYAN, 0.55F));
                }
            }
            float flash = Easing.progress(f.now, r.spentAt[i], 8F);
            if (flash < 1F) HudDraw.cutPanel(g, cx, cy, W, H, 0, 2, 0, 0, f.c(NeonPalette.WHITE, 1F - flash));
        }
        float tx = x + LABEL_W + cap * STRIDE + 3F + shake;
        HudText.draw(g, HudText.micro(ram + "/" + cap), tx, y + 1, f.c(NeonPalette.CYAN_SOFT));
        if (insufficient) {
            Component warn = HudText.micro(Component.translatable("chromeneon.hud.ram.insufficient"));
            HudText.draw(g, warn, tx + 22F, y + 1, f.c(NeonPalette.RED, Easing.blink(f.now, 6F) ? 1F : 0.6F));
        }
    }
}
