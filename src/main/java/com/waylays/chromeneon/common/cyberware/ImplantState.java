package com.waylays.chromeneon.common.cyberware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The working state of one installed implant, saved with the player. Times are server game ticks. Each field has a
 * default so new fields never break old saves.
 *
 * <ul>
 * <li>{@code enabled}: the player's switch (a disabled implant does nothing and gives nothing).</li>
 * <li>{@code active}: currently running (accelerator running, blades deployed, optics analysis on...).</li>
 * <li>{@code charges}: implant-specific uses (pump injections, extra jumps left).</li>
 * <li>{@code resource}: implant-specific gauge (plating integrity).</li>
 * <li>{@code malfunctionUntil}: the implant is down until then (humanity too low).</li>
 * </ul>
 */
public final class ImplantState {
    public static final Codec<ImplantState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(s -> s.enabled),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(s -> s.active),
            Codec.LONG.optionalFieldOf("active_until", 0L).forGetter(s -> s.activeUntil),
            Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(s -> s.cooldownUntil),
            Codec.INT.optionalFieldOf("charges", 0).forGetter(s -> s.charges),
            Codec.FLOAT.optionalFieldOf("resource", 0F).forGetter(s -> s.resource),
            Codec.LONG.optionalFieldOf("malfunction_until", 0L).forGetter(s -> s.malfunctionUntil),
            Codec.LONG.optionalFieldOf("changed_at", 0L).forGetter(s -> s.changedAt)
    ).apply(i, ImplantState::new));

    public boolean enabled = true;
    public boolean active;
    public long activeUntil;
    public long cooldownUntil;
    public int charges;
    public float resource;
    public long malfunctionUntil;
    /** Last time {@code active} changed (for animations on the client). */
    public long changedAt;

    public ImplantState() {}

    private ImplantState(boolean enabled, boolean active, long activeUntil, long cooldownUntil, int charges, float resource,
                         long malfunctionUntil, long changedAt) {
        this.enabled = enabled;
        this.active = active;
        this.activeUntil = activeUntil;
        this.cooldownUntil = cooldownUntil;
        this.charges = charges;
        this.resource = resource;
        this.malfunctionUntil = malfunctionUntil;
        this.changedAt = changedAt;
    }

    public ImplantState copy() {
        return new ImplantState(enabled, active, activeUntil, cooldownUntil, charges, resource, malfunctionUntil, changedAt);
    }

    public boolean malfunctioning(long now) {
        return now < malfunctionUntil;
    }

    public boolean coolingDown(long now) {
        return now < cooldownUntil;
    }

    /** Enabled and not malfunctioning: the implant works. */
    public boolean working(long now) {
        return enabled && !malfunctioning(now);
    }

    public void setActive(boolean value, long now) {
        if (active != value) changedAt = now;
        active = value;
    }
}
