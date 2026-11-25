package com.waylays.chromeneon.client.hud.draw;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector3f;

/** Clipping in local coordinates: the rectangle is carried through the current pose (translation and scale). */
public final class HudScissor {
    private HudScissor() {}

    public static void push(GuiGraphics g, float x0, float y0, float x1, float y1) {
        g.flush();
        Vector3f a = g.pose().last().pose().transformPosition(new Vector3f(x0, y0, 0));
        Vector3f b = g.pose().last().pose().transformPosition(new Vector3f(x1, y1, 0));
        g.enableScissor((int) Math.floor(Math.min(a.x, b.x)), (int) Math.floor(Math.min(a.y, b.y)),
                (int) Math.ceil(Math.max(a.x, b.x)), (int) Math.ceil(Math.max(a.y, b.y)));
    }

    public static void pop(GuiGraphics g) {
        g.flush();
        g.disableScissor();
    }
}
