package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.entity.SeatEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ChromeNeon.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC).sized(0.2F, 0.2F).noSummon().noSave()
                    .clientTrackingRange(8).build("seat"));

    private ModEntities() {}
}
