package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.cyberware.Implant;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.Optional;

/**
 * The implant registry ({@code chromeneon:implant}), synced to clients so both sides agree on ids. Implants are
 * registered objects like items: phase 3 adds them through {@link #IMPLANTS}, and other mods can add their own.
 */
public final class ModImplants {
    public static final ResourceKey<Registry<Implant>> KEY = ResourceKey.createRegistryKey(ChromeNeon.id("implant"));
    public static final Registry<Implant> REGISTRY = new RegistryBuilder<>(KEY).sync(true).create();
    public static final DeferredRegister<Implant> IMPLANTS = DeferredRegister.create(KEY, ChromeNeon.MODID);

    private ModImplants() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((NewRegistryEvent event) -> event.register(REGISTRY));
        IMPLANTS.register(modBus);
    }

    public static Optional<Implant> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }
}
