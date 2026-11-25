package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.implants.ArgusOptics;
import com.waylays.chromeneon.common.cyberware.implants.CarapaceDermis;
import com.waylays.chromeneon.common.cyberware.implants.ChronosAccelerator;
import com.waylays.chromeneon.common.cyberware.implants.HemostatPump;
import com.waylays.chromeneon.common.cyberware.implants.ReaperBlades;
import com.waylays.chromeneon.common.cyberware.implants.SpringTendons;
import com.waylays.chromeneon.common.cyberware.implants.SynapseCoprocessor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.Optional;

/**
 * The implant registry ({@code chromeneon:implant}), synced to clients so both sides agree on ids. Each implant is
 * its own class; other mods can register theirs here and they follow the same rules.
 */
public final class ModImplants {
    public static final ResourceKey<Registry<Implant>> KEY = ResourceKey.createRegistryKey(ChromeNeon.id("implant"));
    public static final Registry<Implant> REGISTRY = new RegistryBuilder<>(KEY).sync(true).create();
    public static final DeferredRegister<Implant> IMPLANTS = DeferredRegister.create(KEY, ChromeNeon.MODID);

    public static final DeferredHolder<Implant, ChronosAccelerator> CHRONOS_ACCELERATOR = IMPLANTS.register("chronos_accelerator", ChronosAccelerator::new);
    public static final DeferredHolder<Implant, ReaperBlades> REAPER_BLADES = IMPLANTS.register("reaper_blades", ReaperBlades::new);
    public static final DeferredHolder<Implant, SpringTendons> SPRING_TENDONS = IMPLANTS.register("spring_tendons", SpringTendons::new);
    public static final DeferredHolder<Implant, CarapaceDermis> CARAPACE_DERMIS = IMPLANTS.register("carapace_dermis", CarapaceDermis::new);
    public static final DeferredHolder<Implant, HemostatPump> HEMOSTAT_PUMP = IMPLANTS.register("hemostat_pump", HemostatPump::new);
    public static final DeferredHolder<Implant, ArgusOptics> ARGUS_OPTICS = IMPLANTS.register("argus_optics", ArgusOptics::new);
    public static final DeferredHolder<Implant, SynapseCoprocessor> SYNAPSE_COPROCESSOR = IMPLANTS.register("synapse_coprocessor", SynapseCoprocessor::new);

    private ModImplants() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((NewRegistryEvent event) -> event.register(REGISTRY));
        IMPLANTS.register(modBus);
    }

    public static Optional<Implant> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static ResourceLocation id(Implant implant) {
        return REGISTRY.getKey(implant);
    }
}
