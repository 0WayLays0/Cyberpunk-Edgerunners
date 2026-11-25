package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.cyberware.BodySlot;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The body slot registry ({@code chromeneon:body_slot}), synced to clients. The body's cybernetic architecture is
 * data, not an enum: other mods (and later phases) add slots by registering them here.
 */
public final class ModBodySlots {
    public static final ResourceKey<Registry<BodySlot>> KEY = ResourceKey.createRegistryKey(ChromeNeon.id("body_slot"));
    public static final Registry<BodySlot> REGISTRY = new RegistryBuilder<>(KEY).sync(true).create();
    public static final DeferredRegister<BodySlot> SLOTS = DeferredRegister.create(KEY, ChromeNeon.MODID);

    public static final ResourceLocation SYSTEM = ChromeNeon.id("system");
    public static final ResourceLocation OPTICS = ChromeNeon.id("optics");
    public static final ResourceLocation BIOCHIP = ChromeNeon.id("biochip");
    public static final ResourceLocation LEFT_ARM = ChromeNeon.id("left_arm");
    public static final ResourceLocation RIGHT_ARM = ChromeNeon.id("right_arm");
    public static final ResourceLocation LEGS = ChromeNeon.id("legs");
    public static final ResourceLocation INTEGUMENTARY = ChromeNeon.id("integumentary");
    public static final ResourceLocation CIRCULATORY = ChromeNeon.id("circulatory");

    static {
        slot(SYSTEM, 1, BodySlot.Region.SPINE);
        slot(OPTICS, 1, BodySlot.Region.EYES);
        slot(BIOCHIP, 1, BodySlot.Region.HEAD);
        slot(LEFT_ARM, 1, BodySlot.Region.LEFT_ARM);
        slot(RIGHT_ARM, 1, BodySlot.Region.RIGHT_ARM);
        slot(LEGS, 1, BodySlot.Region.LEGS);
        slot(INTEGUMENTARY, 2, BodySlot.Region.SKIN);
        slot(CIRCULATORY, 2, BodySlot.Region.HEART);
    }

    private ModBodySlots() {}

    private static DeferredHolder<BodySlot, BodySlot> slot(ResourceLocation id, int sockets, BodySlot.Region region) {
        return SLOTS.register(id.getPath(), () -> new BodySlot(sockets, region));
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((NewRegistryEvent event) -> event.register(REGISTRY));
        SLOTS.register(modBus);
    }
}
