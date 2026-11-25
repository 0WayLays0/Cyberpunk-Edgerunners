package com.waylays.chromeneon.client.hud.anim;

/** A value that follows its target smoothly, independent of frame rate. */
public final class Smoothed {
    private float value;
    private boolean initialized;

    /** Moves toward {@code target}; {@code rate} is how quickly (per tick), {@code dt} the ticks since last frame. */
    public float update(float target, float dt, float rate) {
        if (!initialized) {
            value = target;
            initialized = true;
        } else {
            value += (target - value) * (1F - (float) Math.exp(-rate * Math.max(0F, dt)));
            if (Math.abs(target - value) < 1e-3F) value = target;
        }
        return value;
    }

    public void snap(float target) {
        value = target;
        initialized = true;
    }

    public float get() {
        return value;
    }
}
