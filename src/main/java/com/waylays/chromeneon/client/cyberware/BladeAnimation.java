package com.waylays.chromeneon.client.cyberware;

import java.util.HashMap;
import java.util.Map;

/** How far each player's blades are out, per arm, eased over a few frames when they deploy or retract. */
public final class BladeAnimation {
    private static final float SPEED = 0.18F;
    private static final Map<Long, Float> EXTENSION = new HashMap<>();

    private BladeAnimation() {}

    /** Advances toward {@code deployed} and returns the extension (0 hidden, 1 fully out). */
    public static float extension(int entityId, boolean left, boolean deployed, float delta) {
        long key = ((long) entityId << 1) | (left ? 1 : 0);
        float current = EXTENSION.getOrDefault(key, 0F);
        float target = deployed ? 1F : 0F;
        float next = current + Math.clamp(target - current, -SPEED * delta, SPEED * delta);
        EXTENSION.put(key, next);
        return next;
    }

    public static void clear() {
        EXTENSION.clear();
    }
}
