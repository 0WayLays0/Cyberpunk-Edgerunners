package com.waylays.chromeneon.client.screen;

import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** A slanted outlined button in the interface's language: fills on hover, dims when it cannot be used. */
record NeonButton(float x, float y, float w, float h, Component label, int color, boolean enabled, Runnable action) {
    boolean contains(double mx, double my) {
        return mx >= x && mx <= x + w + 3F && my >= y && my <= y + h;
    }

    void draw(GuiGraphics g, double mx, double my, float px) {
        boolean hover = enabled && contains(mx, my);
        float a = enabled ? 1F : 0.3F;
        if (hover) HudDraw.parallelogram(g, x, y, w, h, 3F, NeonPalette.alpha(color, 0.9F));
        else HudDraw.parallelogram(g, x, y, w, h, 3F, NeonPalette.alpha(color, 0.12F * a));
        HudDraw.parallelogramFrame(g, x, y, w, h, 3F, px, NeonPalette.alpha(color, a));
        Component t = HudText.label(label);
        HudText.drawFlat(g, t, x + 1.5F + (w - HudText.width(t)) / 2F, y + (h - HudText.Kind.LABEL.cap()) / 2F,
                hover ? NeonPalette.VOID : NeonPalette.alpha(color, a));
    }
}
