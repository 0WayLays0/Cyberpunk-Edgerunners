package com.waylays.chromeneon.common.hacking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RamPoolTest {
    @Test
    void consumesOnlyWhatIsAvailable() {
        RamPool pool = new RamPool(4);
        assertTrue(pool.tryConsume(3));
        assertFalse(pool.tryConsume(2), "no hack on partial RAM");
        assertEquals(1, pool.cells());
        assertFalse(pool.tryConsume(-1));
    }

    @Test
    void regeneratesOneCellPerPeriodUpToCapacity() {
        RamPool pool = new RamPool(0);
        for (int t = 0; t < 9; t++) assertFalse(pool.tick(2, 10));
        assertTrue(pool.tick(2, 10));
        assertEquals(1, pool.cells());
        for (int t = 0; t < 100; t++) pool.tick(2, 10);
        assertEquals(2, pool.cells());
    }

    @Test
    void shrinksWhenCapacityDrops() {
        RamPool pool = new RamPool(8);
        assertTrue(pool.tick(4, 10));
        assertEquals(4, pool.cells());
        pool.set(99, 4);
        assertEquals(4, pool.cells());
    }
}
