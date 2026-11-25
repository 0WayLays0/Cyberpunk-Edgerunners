package com.waylays.chromeneon.client.hud.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/** What the scanner is pointed at: a real entity, or a real block of the world. */
public sealed interface ScanSubject {
    /** Identity across ticks: the same target keeps its scan progress. */
    Object key();

    record EntitySubject(Entity entity) implements ScanSubject {
        @Override
        public Object key() {
            return entity.getUUID();
        }
    }

    record BlockSubject(BlockPos pos, BlockState state) implements ScanSubject {
        @Override
        public Object key() {
            return pos;
        }
    }
}
