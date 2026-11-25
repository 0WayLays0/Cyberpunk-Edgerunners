package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.block.RipperdocChairBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ChromeNeon.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RipperdocChairBlockEntity>> RIPPERDOC_CHAIR =
            BLOCK_ENTITIES.register("ripperdoc_chair", () -> BlockEntityType.Builder.of(RipperdocChairBlockEntity::new,
                    ModBlocks.RIPPERDOC_CHAIR.get()).build(null));

    private ModBlockEntities() {}
}
