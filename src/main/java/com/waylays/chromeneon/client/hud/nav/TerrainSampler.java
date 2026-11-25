package com.waylays.chromeneon.client.hud.nav;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

/**
 * The minimap's knowledge of the terrain: a grid of columns around the player, filled from the chunks the client
 * has loaded (the surface heightmap and each top block's map colour). It is refreshed a slice per tick, so a full
 * pass costs nothing noticeable, and slides with the player. Columns in unloaded chunks stay unknown and are drawn
 * as such: the map never invents terrain. In dimensions with a ceiling (the Nether) it reads the floor around the
 * player's height instead of the roof.
 */
public final class TerrainSampler {
    static final byte UNKNOWN = 0, LAND = 1, WATER = 2, LAVA = 3;
    private static final int BUDGET = 4096, CEILING_BUDGET = 1024, CEILING_SCAN = 40;

    private int size, cell = 1, originX, originZ, cursor;
    private int[] color = new int[0];
    private short[] height = new short[0];
    private byte[] kind = new byte[0];
    private ClientLevel level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private long lastNanos;

    /** Keeps the grid covering {@code radius} blocks around ({@code x}, {@code z}), {@code cell} blocks per column. */
    public void tick(ClientLevel level, double x, double y, double z, float radius, int cellSize) {
        long start = System.nanoTime();
        int cells = (int) Math.ceil(radius / cellSize) * 2 + 16;
        if (level != this.level || cellSize != cell || cells > size || cells < size - 32) {
            reset(level, cellSize, cells, x, z);
        }
        int cx = Math.floorDiv((int) Math.floor(x), cell), cz = Math.floorDiv((int) Math.floor(z), cell);
        if (Math.abs(cx - (originX + size / 2)) > size / 8 || Math.abs(cz - (originZ + size / 2)) > size / 8) {
            shift(cx - size / 2, cz - size / 2);
        }
        boolean ceiling = level.dimensionType().hasCeiling();
        int budget = ceiling ? CEILING_BUDGET : BUDGET, total = size * size;
        for (int i = 0; i < budget && total > 0; i++) {
            sample(cursor, ceiling, (int) Math.floor(y));
            cursor = (cursor + 1) % total;
        }
        lastNanos = System.nanoTime() - start;
    }

    private void reset(ClientLevel level, int cellSize, int cells, double x, double z) {
        this.level = level;
        this.cell = cellSize;
        this.size = cells;
        this.color = new int[cells * cells];
        this.height = new short[cells * cells];
        this.kind = new byte[cells * cells];
        this.originX = Math.floorDiv((int) Math.floor(x), cell) - cells / 2;
        this.originZ = Math.floorDiv((int) Math.floor(z), cell) - cells / 2;
        this.cursor = 0;
    }

    /** Moves the grid, keeping what is already known where the old and new areas overlap. */
    private void shift(int newX, int newZ) {
        int n = size;
        int[] c2 = new int[n * n];
        short[] h2 = new short[n * n];
        byte[] k2 = new byte[n * n];
        for (int gz = 0; gz < n; gz++) {
            int oz = gz + newZ - originZ;
            if (oz < 0 || oz >= n) continue;
            for (int gx = 0; gx < n; gx++) {
                int ox = gx + newX - originX;
                if (ox < 0 || ox >= n) continue;
                c2[gz * n + gx] = color[oz * n + ox];
                h2[gz * n + gx] = height[oz * n + ox];
                k2[gz * n + gx] = kind[oz * n + ox];
            }
        }
        color = c2;
        height = h2;
        kind = k2;
        originX = newX;
        originZ = newZ;
        cursor = 0;
    }

    private void sample(int index, boolean ceiling, int playerY) {
        int bx = (originX + index % size) * cell + cell / 2, bz = (originZ + index / size) * cell + cell / 2;
        if (!level.hasChunk(bx >> 4, bz >> 4)) {
            kind[index] = UNKNOWN;
            return;
        }
        int y = ceiling ? floorBelow(bx, playerY, bz) : level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz) - 1;
        if (y < level.getMinBuildHeight()) {
            kind[index] = UNKNOWN;
            return;
        }
        BlockState state = level.getBlockState(pos.set(bx, y, bz));
        FluidState fluid = state.getFluidState();
        height[index] = (short) y;
        if (fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA)) {
            kind[index] = LAVA;
            color[index] = MapPalette.lava();
        } else if (!fluid.isEmpty()) {
            int depth = 0;
            while (depth < 10 && !level.getBlockState(pos.set(bx, y - depth - 1, bz)).getFluidState().isEmpty()) depth++;
            kind[index] = WATER;
            color[index] = MapPalette.water(depth);
        } else {
            MapColor map = state.getMapColor(level, pos);
            if (map == MapColor.NONE) map = level.getBlockState(pos.set(bx, y - 1, bz)).getMapColor(level, pos);
            kind[index] = LAND;
            color[index] = MapPalette.land(map.col);
        }
    }

    /** The first standing surface at or below the player's feet (for dimensions with a roof). */
    private int floorBelow(int x, int fromY, int z) {
        boolean airAbove = level.getBlockState(pos.set(x, fromY + 2, z)).isAir();
        for (int y = fromY + 1; y > fromY - CEILING_SCAN; y--) {
            BlockState s = level.getBlockState(pos.set(x, y, z));
            boolean air = s.isAir();
            if (!air && airAbove) return y;
            airAbove = air;
        }
        return Integer.MIN_VALUE;
    }

    /** Shaded colour of the column holding the world point, or 0 when it is not known. */
    public int colorAt(double wx, double wz) {
        int gx = Math.floorDiv((int) Math.floor(wx), cell) - originX, gz = Math.floorDiv((int) Math.floor(wz), cell) - originZ;
        if (gx < 1 || gz < 1 || gx >= size || gz >= size) return 0;
        int i = gz * size + gx;
        byte k = kind[i];
        if (k == UNKNOWN) return 0;
        if (k != LAND) return color[i];
        int h = height[i], hn = kind[i - size] == UNKNOWN ? h : height[i - size], hw = kind[i - 1] == UNKNOWN ? h : height[i - 1];
        int c = MapPalette.relief(color[i], h, hn, hw);
        return MapPalette.contour(h, hn, hw) ? MapPalette.withContour(c) : c;
    }

    /** Time spent sampling during the last tick, for the performance report. */
    public long lastTickNanos() {
        return lastNanos;
    }

    public void clear() {
        level = null;
        size = 0;
    }
}
