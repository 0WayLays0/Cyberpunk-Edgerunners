package com.waylays.chromeneon.client.hud;

import net.minecraft.client.gui.GuiGraphics;

/** Drawing primitives of the HUD's graphic language: shapes with cut corners rather than plain rectangles. */
public final class HudDraw {
    private HudDraw() {}

    /**
     * Fills a rectangle whose top-left and bottom-right corners are cut at 45 degrees over {@code cut} pixels,
     * drawn as one-pixel rows so the diagonal stays crisp at any GUI scale.
     */
    public static void cutCornerFill(GuiGraphics g, int x, int y, int w, int h, int cut, int color) {
        cut = Math.min(cut, Math.min(w, h) / 2);
        for (int row = 0; row < h; row++) {
            int left = row < cut ? cut - row : 0;
            int fromBottom = h - 1 - row;
            int right = fromBottom < cut ? cut - fromBottom : 0;
            g.fill(x + left, y + row, x + w - right, y + row + 1, color);
        }
    }

    /** Same shape as {@link #cutCornerFill}, as a frame: the fill with an inner fill one pixel smaller on top. */
    public static void cutCornerFrame(GuiGraphics g, int x, int y, int w, int h, int cut, int border, int inside) {
        cutCornerFill(g, x, y, w, h, cut, border);
        cutCornerFill(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, cut - 1), inside);
    }
}
