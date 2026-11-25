package com.waylays.chromeneon.common.cyberware;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CyberwareLoadoutTest {
    private static final ResourceLocation DECK = id("deck"), ACCEL = id("accel"), EYES = id("eyes"),
            EYES2 = id("eyes2"), SKIN_A = id("skin_a"), SKIN_B = id("skin_b"), HEAVY = id("heavy");

    private static final Map<ResourceLocation, Implant> IMPLANTS = Map.of(
            DECK, new Implant(BodySlot.SYSTEM, 20, 4, Optional.of("os")),
            ACCEL, new Implant(BodySlot.SYSTEM, 25, 0, Optional.of("os")),
            EYES, new Implant(BodySlot.OPTICS, 10, 0, Optional.empty()),
            EYES2, new Implant(BodySlot.OPTICS, 10, 0, Optional.empty()),
            SKIN_A, new Implant(BodySlot.INTEGUMENTARY, 30, 0, Optional.empty()),
            SKIN_B, new Implant(BodySlot.INTEGUMENTARY, 30, 0, Optional.empty()),
            HEAVY, new Implant(BodySlot.CIRCULATORY, 50, 0, Optional.empty()));
    private static final Function<ResourceLocation, Optional<Implant>> LOOKUP = rl -> Optional.ofNullable(IMPLANTS.get(rl));

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    @Test
    void installsIntoItsSlotAndCostsHumanity() {
        CyberwareLoadout l = new CyberwareLoadout();
        assertEquals(InstallResult.OK, l.install(DECK, LOOKUP, 100));
        assertEquals(java.util.List.of(DECK), l.in(BodySlot.SYSTEM));
        assertEquals(20, l.humanityCost(LOOKUP));
    }

    @Test
    void refusesUnknownDuplicateFullConflictAndHumanity() {
        CyberwareLoadout l = new CyberwareLoadout();
        assertEquals(InstallResult.UNKNOWN_IMPLANT, l.install(id("nope"), LOOKUP, 100));
        l.install(EYES, LOOKUP, 100);
        assertEquals(InstallResult.ALREADY_INSTALLED, l.install(EYES, LOOKUP, 100));
        assertEquals(InstallResult.SLOT_FULL, l.install(EYES2, LOOKUP, 100));
        l.install(DECK, LOOKUP, 100);
        assertEquals(InstallResult.CONFLICT, l.install(ACCEL, LOOKUP, 100));
        l.install(SKIN_A, LOOKUP, 100);
        assertEquals(InstallResult.NOT_ENOUGH_HUMANITY, l.install(SKIN_B, LOOKUP, 89));
        assertFalse(l.contains(SKIN_B), "a refused install changes nothing");
        assertEquals(InstallResult.OK, l.install(SKIN_B, LOOKUP, 90));
    }

    @Test
    void removalFreesTheSlotAndUnknownIdsAreKeptButIgnored() {
        CyberwareLoadout l = new CyberwareLoadout();
        l.install(EYES, LOOKUP, 100);
        assertTrue(l.remove(EYES));
        assertFalse(l.remove(EYES));
        assertEquals(InstallResult.OK, l.install(EYES2, LOOKUP, 100));
        // An implant whose mod was removed: still listed, costs nothing
        Function<ResourceLocation, Optional<Implant>> without = rl -> rl.equals(EYES2) ? Optional.empty() : LOOKUP.apply(rl);
        assertTrue(l.contains(EYES2));
        assertEquals(0, l.humanityCost(without));
    }
}
