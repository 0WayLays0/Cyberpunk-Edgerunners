package com.waylays.chromeneon.client.hud;

import java.util.HashMap;
import java.util.Map;

/** Where each HUD group was last drawn, in GUI pixels: lets the automated test check what the HUD covers. */
public final class HudBounds {
    public record Rect(float x0, float y0, float x1, float y1) {
        public boolean intersects(Rect o) {
            return x0 < o.x1 && o.x0 < x1 && y0 < o.y1 && o.y0 < y1;
        }
    }

    private static final Map<String, Rect> LAST = new HashMap<>();
    private static Map<String, Rect> previous = Map.of();
    private static long frames;

    private HudBounds() {}

    static void put(String group, float x, float y, float w, float h) {
        LAST.put(group, new Rect(x, y, x + w, y + h));
    }

    /** Where the groups were in the previous frame: for layers drawn before them that must avoid them. */
    public static Map<String, Rect> previous() {
        return previous;
    }

    public static Map<String, Rect> snapshot() {
        return Map.copyOf(LAST);
    }

    /** Called at the start of each frame: only what is drawn in this frame is listed. */
    static void newFrame() {
        previous = Map.copyOf(LAST);
        LAST.clear();
        frames++;
    }

    /** Frames rendered so far: lets the test tell a frozen window from an empty HUD. */
    public static long frames() {
        return frames;
    }
}
