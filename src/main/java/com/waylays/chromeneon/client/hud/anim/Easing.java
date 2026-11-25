package com.waylays.chromeneon.client.hud.anim;

/** Easing curves for HUD transitions. All take and return 0..1. */
public final class Easing {
    private Easing() {}

    public static float clamp(float t) {
        return Math.clamp(t, 0F, 1F);
    }

    public static float outCubic(float t) {
        float k = 1F - clamp(t);
        return 1F - k * k * k;
    }

    public static float inCubic(float t) {
        float k = clamp(t);
        return k * k * k;
    }

    public static float inOutCubic(float t) {
        float k = clamp(t);
        return k < 0.5F ? 4F * k * k * k : 1F - (float) Math.pow(-2F * k + 2F, 3) / 2F;
    }

    /** Overshoots slightly before settling: for elements that snap into place. */
    public static float outBack(float t) {
        float k = clamp(t) - 1F, s = 1.70158F;
        return 1F + (s + 1F) * k * k * k + s * k * k;
    }

    public static float smoothstep(float t) {
        float k = clamp(t);
        return k * k * (3F - 2F * k);
    }

    /** Progress of a transition that started at {@code start} and lasts {@code duration}, at {@code now}. */
    public static float progress(float now, float start, float duration) {
        return duration <= 0 ? 1F : clamp((now - start) / duration);
    }

    /** A 0..1 pulse of the given period, in ticks. */
    public static float pulse(float now, float period) {
        return 0.5F + 0.5F * (float) Math.sin(now / period * Math.PI * 2);
    }

    /** True during the first half of each period: blinking. */
    public static boolean blink(float now, float period) {
        return (now % period) < period / 2F;
    }

    /**
     * A heartbeat: two quick beats then rest, 0..1. {@code period} shortens as danger grows.
     */
    public static float heartbeat(float now, float period) {
        float p = (now % period) / period;
        float a = Math.max(0F, 1F - Math.abs(p - 0.08F) / 0.08F);
        float b = Math.max(0F, 1F - Math.abs(p - 0.30F) / 0.08F) * 0.7F;
        return Math.max(a, b);
    }
}
