package com.waylays.chromeneon.client.hud.draw;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * The HUD's drawing primitives, built from real polygons (not stacks of rectangles): cut-corner panels, chevrons,
 * parallelograms, brackets, arcs, gradients. Coordinates are floats in GUI pixels, so animations move smoothly.
 * Every polygon is wound the way the GUI pipeline expects whatever order its points are given in.
 */
public final class HudDraw {
    private HudDraw() {}

    private static VertexConsumer buffer(GuiGraphics g) {
        return g.bufferSource().getBuffer(RenderType.gui());
    }

    /** One quad with a colour per corner, wound correctly whatever the order of its corners. */
    public static void quad(GuiGraphics g, float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy,
                            int ca, int cb, int cc, int cd) {
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = buffer(g);
        float area = (ax * by - bx * ay) + (bx * cy - cx * by) + (cx * dy - dx * cy) + (dx * ay - ax * dy);
        if (area <= 0) {
            vc.addVertex(m, ax, ay, 0).setColor(ca);
            vc.addVertex(m, bx, by, 0).setColor(cb);
            vc.addVertex(m, cx, cy, 0).setColor(cc);
            vc.addVertex(m, dx, dy, 0).setColor(cd);
        } else {
            vc.addVertex(m, dx, dy, 0).setColor(cd);
            vc.addVertex(m, cx, cy, 0).setColor(cc);
            vc.addVertex(m, bx, by, 0).setColor(cb);
            vc.addVertex(m, ax, ay, 0).setColor(ca);
        }
    }

    public static void quad(GuiGraphics g, float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int c) {
        quad(g, ax, ay, bx, by, cx, cy, dx, dy, c, c, c, c);
    }

    public static void triangle(GuiGraphics g, float ax, float ay, float bx, float by, float cx, float cy, int c) {
        quad(g, ax, ay, bx, by, cx, cy, cx, cy, c);
    }

    public static void rect(GuiGraphics g, float x0, float y0, float x1, float y1, int c) {
        if (x1 <= x0 || y1 <= y0 || (c >>> 24) == 0) return;
        quad(g, x0, y0, x0, y1, x1, y1, x1, y0, c);
    }

    public static void hLine(GuiGraphics g, float x0, float x1, float y, int c) {
        rect(g, Math.min(x0, x1), y, Math.max(x0, x1), y + 1, c);
    }

    public static void vLine(GuiGraphics g, float x, float y0, float y1, int c) {
        rect(g, x, Math.min(y0, y1), x + 1, Math.max(y0, y1), c);
    }

    /** A straight line of any angle, {@code thickness} pixels wide. */
    public static void line(GuiGraphics g, float x0, float y0, float x1, float y1, float thickness, int c) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-3F) return;
        float nx = -dy / len * thickness / 2F, ny = dx / len * thickness / 2F;
        quad(g, x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, c);
    }

    /** Convex polygon from points {x0, y0, x1, y1, ...} in order around it. */
    public static void polygon(GuiGraphics g, float[] p, int c) {
        int n = p.length / 2;
        for (int i = 1; i + 1 < n; i += 2) {
            int k = Math.min(i + 2, n - 1);
            quad(g, p[0], p[1], p[2 * i], p[2 * i + 1], p[2 * (i + 1)], p[2 * (i + 1) + 1], p[2 * k], p[2 * k + 1], c);
        }
    }

    /** The outline of a convex polygon, {@code t} pixels thick, drawn inward. */
    public static void outline(GuiGraphics g, float[] p, float t, int c) {
        int n = p.length / 2;
        float cx = 0, cy = 0;
        for (int i = 0; i < n; i++) {
            cx += p[2 * i];
            cy += p[2 * i + 1];
        }
        cx /= n;
        cy /= n;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float ax = p[2 * i], ay = p[2 * i + 1], bx = p[2 * j], by = p[2 * j + 1];
            float ex = bx - ax, ey = by - ay, len = (float) Math.sqrt(ex * ex + ey * ey);
            if (len < 1e-3F) continue;
            float nx = -ey / len * t, ny = ex / len * t;
            // Point the offset toward the inside of the polygon
            if ((cx - ax) * nx + (cy - ay) * ny < 0) {
                nx = -nx;
                ny = -ny;
            }
            quad(g, ax, ay, bx, by, bx + nx, by + ny, ax + nx, ay + ny, c);
        }
    }

    /** The points of a box whose corners are cut at 45 degrees: top-left, top-right, bottom-right, bottom-left. */
    public static float[] cutBox(float x, float y, float w, float h, float tl, float tr, float br, float bl) {
        return new float[] {
                x + tl, y, x + w - tr, y, x + w, y + tr, x + w, y + h - br,
                x + w - br, y + h, x + bl, y + h, x, y + h - bl, x, y + tl};
    }

    public static void cutPanel(GuiGraphics g, float x, float y, float w, float h, float tl, float tr, float br, float bl, int c) {
        polygon(g, cutBox(x, y, w, h, tl, tr, br, bl), c);
    }

    public static void cutFrame(GuiGraphics g, float x, float y, float w, float h, float tl, float tr, float br, float bl, int c) {
        outline(g, cutBox(x, y, w, h, tl, tr, br, bl), 1F, c);
    }

    /** A parallelogram leaning right by {@code skew} pixels at its top. */
    public static void parallelogram(GuiGraphics g, float x, float y, float w, float h, float skew, int c) {
        quad(g, x + skew, y, x, y + h, x + w, y + h, x + w + skew, y, c);
    }

    /** A chevron pointing right: {@code w} thick, {@code depth} deep, {@code h} tall. */
    public static void chevron(GuiGraphics g, float x, float y, float w, float h, float depth, int c) {
        float mid = y + h / 2F;
        quad(g, x, y, x + depth, mid, x + depth + w, mid, x + w, y, c);
        quad(g, x + depth, mid, x, y + h, x + w, y + h, x + depth + w, mid, c);
    }

    /** Four L-shaped corners around a rectangle. */
    public static void brackets(GuiGraphics g, float x0, float y0, float x1, float y1, float len, float t, int c) {
        rect(g, x0, y0, x0 + len, y0 + t, c);
        rect(g, x0, y0, x0 + t, y0 + len, c);
        rect(g, x1 - len, y0, x1, y0 + t, c);
        rect(g, x1 - t, y0, x1, y0 + len, c);
        rect(g, x0, y1 - t, x0 + len, y1, c);
        rect(g, x0, y1 - len, x0 + t, y1, c);
        rect(g, x1 - len, y1 - t, x1, y1, c);
        rect(g, x1 - t, y1 - len, x1, y1, c);
    }

    public static void vGradient(GuiGraphics g, float x0, float y0, float x1, float y1, int top, int bottom) {
        quad(g, x0, y0, x0, y1, x1, y1, x1, y0, top, bottom, bottom, top);
    }

    public static void hGradient(GuiGraphics g, float x0, float y0, float x1, float y1, int left, int right) {
        quad(g, x0, y0, x0, y1, x1, y1, x1, y0, left, left, right, right);
    }

    /**
     * A ring segment between {@code a0} and {@code a1} (radians, 0 = up, clockwise), from radius {@code r} outward by
     * {@code t}, as {@code steps} quads.
     */
    public static void arc(GuiGraphics g, float cx, float cy, float r, float t, float a0, float a1, int steps, int c) {
        for (int i = 0; i < steps; i++) {
            float u = a0 + (a1 - a0) * i / steps, v = a0 + (a1 - a0) * (i + 1) / steps;
            float su = (float) Math.sin(u), cu = (float) -Math.cos(u), sv = (float) Math.sin(v), cv = (float) -Math.cos(v);
            quad(g, cx + su * r, cy + cu * r, cx + su * (r + t), cy + cu * (r + t),
                    cx + sv * (r + t), cy + cv * (r + t), cx + sv * r, cy + cv * r, c);
        }
    }

    /** Diagonal hazard stripes inside a rectangle (each stripe clamped to the rectangle's sides). */
    public static void stripes(GuiGraphics g, float x, float y, float w, float h, float period, int c) {
        float band = period / 2F, x1 = x + w;
        for (float s = x - h; s < x1; s += period) {
            float bl = Math.clamp(s, x, x1), br = Math.clamp(s + band, x, x1);
            float tl = Math.clamp(s + h, x, x1), tr = Math.clamp(s + h + band, x, x1);
            if (br - bl < 0.01F && tr - tl < 0.01F) continue;
            quad(g, tl, y, bl, y + h, br, y + h, tr, y, c);
        }
    }

    /** A dashed horizontal line, {@code t} thick. */
    public static void dashes(GuiGraphics g, float x0, float x1, float y, float dash, float gap, float t, int c) {
        for (float x = x0; x < x1; x += dash + gap) rect(g, x, y, Math.min(x1, x + dash), y + t, c);
    }

    /** A polyline {@code t} wide through points {x0, y0, x1, y1, ...}, with square joins. */
    public static void stroke(GuiGraphics g, float[] p, float t, int c) {
        for (int i = 0; i + 3 < p.length; i += 2) {
            line(g, p[i], p[i + 1], p[i + 2], p[i + 3], t, c);
            if (i > 0) rect(g, p[i] - t / 2F, p[i + 1] - t / 2F, p[i] + t / 2F, p[i + 1] + t / 2F, c);
        }
    }

    /** A full ring. */
    public static void ring(GuiGraphics g, float cx, float cy, float r, float t, int steps, int c) {
        arc(g, cx, cy, r, t, 0F, (float) (Math.PI * 2), steps, c);
    }

    /** A ring cut into {@code dashes} arcs, turned by {@code phase} radians. */
    public static void dashedRing(GuiGraphics g, float cx, float cy, float r, float t, int dashes, float fill, float phase, int c) {
        float step = (float) (Math.PI * 2 / dashes);
        for (int i = 0; i < dashes; i++) {
            float a0 = phase + i * step;
            arc(g, cx, cy, r, t, a0, a0 + step * fill, 3, c);
        }
    }

    /** A small diamond: the nodes of rails and leader lines. */
    public static void diamond(GuiGraphics g, float x, float y, float r, int c) {
        quad(g, x, y - r, x - r, y, x, y + r, x + r, y, c);
    }

    /** A parallelogram with a vertical gradient (light top, darker bottom): segments with a sheen. */
    public static void parallelogram(GuiGraphics g, float x, float y, float w, float h, float skew, int top, int bottom) {
        quad(g, x + skew, y, x, y + h, x + w, y + h, x + w + skew, y, top, bottom, bottom, top);
    }

    /** Outline of a parallelogram, {@code t} thick. */
    public static void parallelogramFrame(GuiGraphics g, float x, float y, float w, float h, float skew, float t, int c) {
        outline(g, new float[] {x + skew, y, x + w + skew, y, x + w, y + h, x, y + h}, t, c);
    }

    /** A soft backing that fades out to the right: replaces closed panels. */
    public static void fadeRight(GuiGraphics g, float x0, float y0, float x1, float y1, int c) {
        hGradient(g, x0, y0, x1, y1, c, c & 0x00FFFFFF);
    }

    public static void fadeLeft(GuiGraphics g, float x0, float y0, float x1, float y1, int c) {
        hGradient(g, x0, y0, x1, y1, c & 0x00FFFFFF, c);
    }

    /**
     * A veil: a dark backing with feathered top and bottom edges, going from {@code left} to {@code right} across.
     * It sits behind a block of data to keep it readable on any background, without the edges of a panel.
     */
    public static void veil(GuiGraphics g, float x0, float y0, float x1, float y1, float feather, int left, int right) {
        int l0 = left & 0x00FFFFFF, r0 = right & 0x00FFFFFF;
        float f = Math.min(feather, (y1 - y0) / 2F);
        quad(g, x0, y0, x0, y0 + f, x1, y0 + f, x1, y0, l0, left, right, r0);
        quad(g, x0, y0 + f, x0, y1 - f, x1, y1 - f, x1, y0 + f, left, left, right, right);
        quad(g, x0, y1 - f, x0, y1, x1, y1, x1, y1 - f, left, l0, r0, right);
    }
}
