package com.waylays.chromeneon.client.hud;

import com.waylays.chromeneon.common.config.ClientConfig;
import com.waylays.chromeneon.common.network.SyncCyberStatePayload;
import com.waylays.chromeneon.common.player.SyncedCyberState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * RAM as physical cells, right of the hotbar: lit cyan when available, dim when spent. The first HUD element and
 * the reference for the others (phase 2 builds the full HUD on the same primitives).
 */
public final class RamCellsLayer implements LayeredDraw.Layer {
    private static final int CELL_W = 5, CELL_H = 9, GAP = 2, PAD = 3, HOTBAR_HALF = 91, MARGIN = 8;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        SyncCyberStatePayload state = SyncedCyberState.current();
        if (!ClientConfig.HUD_ENABLED.get() || state == null || mc.options.hideGui || mc.player == null
                || mc.player.isSpectator()) {
            return;
        }
        int capacity = state.stats().ramCapacity();
        if (capacity <= 0) return;
        double opacity = ClientConfig.HUD_OPACITY.get();

        int width = PAD * 2 + capacity * CELL_W + (capacity - 1) * GAP;
        int height = PAD * 2 + CELL_H;
        int x = g.guiWidth() / 2 + HOTBAR_HALF + MARGIN;
        int y = g.guiHeight() - height - 2;
        HudDraw.cutCornerFrame(g, x, y, width, height, 4,
                NeonPalette.fade(NeonPalette.VIOLET_DIM, opacity), NeonPalette.fade(NeonPalette.PANEL, opacity * 0.85));

        int lit = state.stats().ram();
        for (int i = 0; i < capacity; i++) {
            int cx = x + PAD + i * (CELL_W + GAP);
            int color = i < lit ? NeonPalette.CYAN : NeonPalette.CYAN_DIM;
            HudDraw.cutCornerFill(g, cx, y + PAD, CELL_W, CELL_H, 2, NeonPalette.fade(color, opacity));
        }
    }
}
