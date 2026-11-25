package com.waylays.chromeneon.client.hud.fx;

import com.waylays.chromeneon.client.hud.HudTime;
import com.waylays.chromeneon.common.config.ClientConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * The one source of glitches: heavy damage, alerts, and later hacks or malfunctions ask for one here. A glitch
 * always ends (at most 20 ticks) and is followed by a cooldown, so it can never become permanent.
 */
public final class GlitchController {
    public static final int MAX_TICKS = 20;
    private static final int COOLDOWN = 15;

    /** A horizontal slice of the screen pushed sideways: position and height as fractions of the screen. */
    public record Band(float y, float height, float shift) {}

    private float start = -1000, end = -1000, intensity, cooldownUntil = -1000;

    /** Requests a glitch of {@code intensity} (0..1) for {@code ticks}. Ignored when glitches are turned off. */
    public void trigger(float intensity, int ticks) {
        if (!ClientConfig.GLITCH_EFFECTS.get()) return;
        float now = HudTime.ticks();
        boolean running = now < end;
        if (!running && now < cooldownUntil) return;
        if (running && intensity <= this.intensity) return;
        this.start = now;
        this.end = now + Math.clamp(ticks, 2, MAX_TICKS);
        this.intensity = Math.clamp(intensity, 0F, 1F);
        this.cooldownUntil = end + COOLDOWN;
    }

    /** Current strength, 0 when no glitch runs. It stutters: strong frames alternate with calm ones. */
    public float strength(float now) {
        if (now < start || now >= end || !ClientConfig.GLITCH_EFFECTS.get()) return 0F;
        float t = (now - start) / (end - start);
        float decay = 1F - t * t;
        boolean calm = hash((int) Math.floor(now / 1.5F), 7) < 0.35F;
        return intensity * decay * (calm ? 0.25F : 1F);
    }

    /** A small sideways shake for the HUD element identified by {@code seed}. */
    public float jitter(int seed, float now) {
        float s = strength(now);
        if (s <= 0F) return 0F;
        int step = (int) Math.floor(now / 1.5F);
        if (hash(step, seed) < 0.5F) return 0F;
        return (hash(step, seed ^ 0x5bd1e995) * 2F - 1F) * 4F * s;
    }

    /** The slices of screen torn this frame (they change every two ticks). */
    public List<Band> bands(float now) {
        float s = strength(now);
        List<Band> bands = new ArrayList<>();
        if (s <= 0F) return bands;
        int step = (int) Math.floor(now / 2F);
        int count = 2 + (int) (hash(step, 1) * 4);
        for (int i = 0; i < count; i++) {
            float y = hash(step, 10 + i);
            float h = 0.006F + hash(step, 20 + i) * 0.05F * s;
            float shift = (hash(step, 30 + i) * 2F - 1F) * s;
            bands.add(new Band(y, h, shift));
        }
        return bands;
    }

    /** Deterministic noise in 0..1. */
    static float hash(int a, int b) {
        int h = a * 0x27d4eb2d ^ b * 0x165667b1;
        h ^= h >>> 15;
        h *= 0x2c1b3c6d;
        h ^= h >>> 12;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }
}
