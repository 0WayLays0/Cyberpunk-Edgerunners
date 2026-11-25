package com.waylays.chromeneon.common.cyberware.mechanics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The numbers behind the implants, without a game around them. */
class CyberMathTest {
    @Test
    void platingTakesItsShareUntilItBreaks() {
        assertArrayEquals(new float[] {6F, 16F}, CyberMath.absorb(10F, 20F, 0.4F), 1e-6F, "40% of 10 taken, 16 left");
        assertArrayEquals(new float[] {7F, 0F}, CyberMath.absorb(10F, 3F, 0.4F), 1e-6F, "only what is left of it");
        assertArrayEquals(new float[] {10F, 0F}, CyberMath.absorb(10F, 0F, 0.4F), 1e-6F, "breached: nothing taken");
    }

    @Test
    void pumpInjectsOnlyWhenItShould() {
        assertTrue(CyberMath.shouldInject(5F, 20F, 0.3F, 1, false));
        assertFalse(CyberMath.shouldInject(7F, 20F, 0.3F, 1, false), "above the threshold");
        assertFalse(CyberMath.shouldInject(5F, 20F, 0.3F, 0, false), "no charge");
        assertFalse(CyberMath.shouldInject(5F, 20F, 0.3F, 2, true), "cooling down");
        assertFalse(CyberMath.shouldInject(0F, 20F, 0.3F, 2, false), "dead");
    }

    @Test
    void malfunctionsOnlyUnderTheThreshold() {
        assertEquals(0.0, CyberMath.malfunctionChance(0.5F, 0.25, 0.5, 3));
        assertEquals(0.0, CyberMath.malfunctionChance(0.25F, 0.25, 0.5, 3), "at the threshold: none");
        double atZero = CyberMath.malfunctionChance(0F, 0.25, 0.5, 3);
        double half = CyberMath.malfunctionChance(0.125F, 0.25, 0.5, 3);
        assertTrue(atZero > half && half > 0, "grows as humanity falls");
        // Three checks a minute at the per-check chance give the configured chance per minute
        assertEquals(0.5, 1 - Math.pow(1 - atZero, 3), 1e-9);
    }

    @Test
    void airJumpsNeedTheAirAndACharge() {
        assertTrue(CyberMath.canAirJump(false, false, false, false, 1));
        assertFalse(CyberMath.canAirJump(true, false, false, false, 1), "on the ground: a normal jump");
        assertFalse(CyberMath.canAirJump(false, true, false, false, 1), "flying");
        assertFalse(CyberMath.canAirJump(false, false, true, false, 1), "swimming");
        assertFalse(CyberMath.canAirJump(false, false, false, true, 1), "riding");
        assertFalse(CyberMath.canAirJump(false, false, false, false, 0), "no jump left");
    }

    @Test
    void dilatedEntitiesLiveOneTickInN() {
        for (int slowdown : new int[] {2, 3, 4, 6}) {
            for (int id = 0; id < 5; id++) {
                int lived = 0;
                for (long t = 0; t < 120; t++) if (CyberMath.livesThisTick(t, id, slowdown)) lived++;
                assertEquals(120 / slowdown, lived, "slowdown " + slowdown + ", entity " + id);
            }
        }
        assertTrue(CyberMath.livesThisTick(7, 3, 1), "no slowdown: every tick");
        // Staggered: not every entity moves on the same tick
        assertTrue(CyberMath.livesThisTick(0, 0, 4) && !CyberMath.livesThisTick(0, 1, 4));
    }

    @Test
    void cooldownReductions() {
        assertEquals(425, CyberMath.cooldown(500, 0.85));
        assertEquals(500, CyberMath.cooldown(500, 1.0));
        assertEquals(0, CyberMath.cooldown(500, -2.0), "never negative");
        assertEquals(361, CyberMath.cooldown(500, 0.85 * 0.85), "factors stack");
    }

    @Test
    void criticalHits() {
        assertEquals(1.5F, CyberMath.critMultiplier(0.05, 0.10, 0.5));
        assertEquals(1F, CyberMath.critMultiplier(0.10, 0.10, 0.5), "the roll must be under the chance");
        assertEquals(2F, CyberMath.critMultiplier(0.0, 0.15, 1.0));
        assertEquals(1F, CyberMath.critMultiplier(0.0, 0.0, 1.0), "no chance, no crit");
    }
}
