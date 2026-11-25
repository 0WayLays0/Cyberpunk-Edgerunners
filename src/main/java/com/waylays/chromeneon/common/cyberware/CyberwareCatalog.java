package com.waylays.chromeneon.common.cyberware;

import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Looks up implants and body slots by id. The game uses the registries; tests use their own catalogue. */
public interface CyberwareCatalog {
    Optional<Implant> implant(ResourceLocation id);

    Optional<BodySlot> slot(ResourceLocation id);

    CyberwareCatalog REGISTRIES = new CyberwareCatalog() {
        @Override
        public Optional<Implant> implant(ResourceLocation id) {
            return Optional.ofNullable(ModImplants.REGISTRY.get(id));
        }

        @Override
        public Optional<BodySlot> slot(ResourceLocation id) {
            return Optional.ofNullable(ModBodySlots.REGISTRY.get(id));
        }
    };
}
