package com.waylays.chromeneon.common.cyberware;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A catalogue of made-up slots and implants, so the rules can be tested without the game's registries. */
public class TestCatalog implements CyberwareCatalog {
    public static final ResourceLocation SYSTEM = id("system"), OPTICS = id("optics"), ARM_L = id("arm_l"), ARM_R = id("arm_r"),
            SKIN = id("skin"), BIOCHIP = id("biochip");
    public static final ResourceLocation DECK = id("deck"), ACCEL = id("accel"), EYES = id("eyes"), EYES2 = id("eyes2"),
            BLADES = id("blades"), PLATE = id("plate"), HEAVY = id("heavy"), CHIP = id("chip");

    private final Map<ResourceLocation, Implant> implants = new HashMap<>();
    private final Map<ResourceLocation, BodySlot> slots = new HashMap<>();

    /** An implant with no mechanics of its own. */
    public static final class Plain extends Implant {
        public Plain(Properties properties) {
            super(properties);
        }

        @Override
        public List<Component> describe() {
            return List.of();
        }
    }

    public TestCatalog() {
        slots.put(SYSTEM, new BodySlot(1, BodySlot.Region.SPINE));
        slots.put(OPTICS, new BodySlot(1, BodySlot.Region.EYES));
        slots.put(ARM_L, new BodySlot(1, BodySlot.Region.LEFT_ARM));
        slots.put(ARM_R, new BodySlot(1, BodySlot.Region.RIGHT_ARM));
        slots.put(SKIN, new BodySlot(2, BodySlot.Region.SKIN));
        slots.put(BIOCHIP, new BodySlot(1, BodySlot.Region.HEAD));
        implants.put(DECK, new Plain(Implant.Properties.of(Implant.Mode.PASSIVE, 20, 6, SYSTEM).exclusive(Implant.OPERATING_SYSTEM).ram(4)));
        implants.put(ACCEL, new Plain(Implant.Properties.of(Implant.Mode.TRIGGERED, 25, 8, SYSTEM).exclusive(Implant.OPERATING_SYSTEM)));
        implants.put(EYES, new Plain(Implant.Properties.of(Implant.Mode.TOGGLE, 8, 4, OPTICS)));
        implants.put(EYES2, new Plain(Implant.Properties.of(Implant.Mode.TOGGLE, 8, 4, OPTICS)));
        implants.put(BLADES, new Plain(Implant.Properties.of(Implant.Mode.TOGGLE, 12, 6, ARM_L, ARM_R).perBody(2)));
        implants.put(PLATE, new Plain(Implant.Properties.of(Implant.Mode.PASSIVE, 15, 6, SKIN)));
        implants.put(HEAVY, new Plain(Implant.Properties.of(Implant.Mode.PASSIVE, 60, 4, SKIN)));
        implants.put(CHIP, new Plain(Implant.Properties.of(Implant.Mode.PASSIVE, 8, 4, BIOCHIP).ram(2)));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    @Override
    public Optional<Implant> implant(ResourceLocation id) {
        return Optional.ofNullable(implants.get(id));
    }

    @Override
    public Optional<BodySlot> slot(ResourceLocation id) {
        return Optional.ofNullable(slots.get(id));
    }
}
