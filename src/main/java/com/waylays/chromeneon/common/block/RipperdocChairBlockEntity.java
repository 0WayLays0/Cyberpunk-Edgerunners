package com.waylays.chromeneon.common.block;

import com.waylays.chromeneon.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The chair's block entity: it carries no data, it gives the chair its animated hologram renderer. */
public class RipperdocChairBlockEntity extends BlockEntity {
    public RipperdocChairBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RIPPERDOC_CHAIR.get(), pos, state);
    }
}
