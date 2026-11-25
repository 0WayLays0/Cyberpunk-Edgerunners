package com.waylays.chromeneon.common.cyberware.mechanics;

/** The numbers behind the implants, as pure functions (tested without the game). */
public final class CyberMath {
    private CyberMath() {}

    /**
     * Plating absorption: the plating takes {@code share} of a hit, limited by its integrity.
     *
     * @return {damage left for the body, integrity left}
     */
    public static float[] absorb(float damage, float integrity, float share) {
        float taken = Math.min(Math.max(0F, integrity), Math.max(0F, damage) * Math.clamp(share, 0F, 1F));
        return new float[] {damage - taken, integrity - taken};
    }

    /** Whether the pump injects: health at or under the threshold, alive, a charge left, not cooling down. */
    public static boolean shouldInject(float health, float maxHealth, float threshold, int charges, boolean coolingDown) {
        return health > 0F && health <= maxHealth * threshold && charges > 0 && !coolingDown;
    }

    /**
     * Chance that an implant malfunctions at one check, given how strained the body is: 0 at or above the
     * threshold, rising linearly to the per-minute chance at zero humanity.
     */
    public static double malfunctionChance(float humanityRatio, double threshold, double chancePerMinute, int checksPerMinute) {
        if (threshold <= 0 || humanityRatio >= threshold) return 0.0;
        double severity = 1.0 - humanityRatio / threshold;
        double perMinute = Math.clamp(chancePerMinute * severity, 0.0, 1.0);
        return 1.0 - Math.pow(1.0 - perMinute, 1.0 / checksPerMinute);
    }

    /** Whether an extra jump is possible from this state of the player. */
    public static boolean canAirJump(boolean onGround, boolean flying, boolean inLiquid, boolean riding, int jumpsLeft) {
        return !onGround && !flying && !inLiquid && !riding && jumpsLeft > 0;
    }

    /** Whether an entity in a dilated zone lives this tick: one tick out of {@code slowdown}, staggered by id. */
    public static boolean livesThisTick(long gameTime, int entityId, int slowdown) {
        return slowdown <= 1 || Math.floorMod(gameTime + entityId, slowdown) == 0;
    }

    /** A cooldown after the multipliers of other implants, in whole ticks, never negative. */
    public static int cooldown(int base, double factor) {
        return (int) Math.max(0, Math.round(base * Math.max(0.0, factor)));
    }

    /**
     * Damage multiplier of a hit while the accelerator runs: a roll in [0, 1) under {@code chance} makes it critical,
     * adding {@code bonus} (0.5 = +50%).
     */
    public static float critMultiplier(double roll, double chance, double bonus) {
        return roll < chance ? 1F + (float) bonus : 1F;
    }
}
