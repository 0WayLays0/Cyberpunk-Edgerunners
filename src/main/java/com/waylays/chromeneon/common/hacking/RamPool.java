package com.waylays.chromeneon.common.hacking;

/**
 * The RAM spent by quickhacks: whole cells, recovered one at a time. Capacity is not stored here, it is passed in
 * (it depends on config and implants), so the pool can never disagree with the body it belongs to.
 */
public final class RamPool {
    private int cells;
    private int regenProgress;

    public RamPool(int cells) {
        this.cells = Math.max(0, cells);
    }

    public int cells() {
        return cells;
    }

    /** Advances regeneration by one tick. Returns true when the number of cells changed. */
    public boolean tick(int capacity, int ticksPerCell) {
        if (cells > capacity) {
            cells = capacity;
            regenProgress = 0;
            return true;
        }
        if (cells == capacity) {
            regenProgress = 0;
            return false;
        }
        if (++regenProgress >= Math.max(1, ticksPerCell)) {
            regenProgress = 0;
            cells++;
            return true;
        }
        return false;
    }

    /** Spends cells if all of them are available; a hack never runs on partial RAM. */
    public boolean tryConsume(int cost) {
        if (cost < 0 || cost > cells) return false;
        cells -= cost;
        return true;
    }

    public void set(int value, int capacity) {
        cells = Math.clamp(value, 0, capacity);
        regenProgress = 0;
    }
}
