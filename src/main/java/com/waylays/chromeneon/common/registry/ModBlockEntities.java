package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity types. Empty in phase 1: the ripperdoc chair gets its block entity with the install flow (phase 3). */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ChromeNeon.MODID);

    private ModBlockEntities() {}
}
