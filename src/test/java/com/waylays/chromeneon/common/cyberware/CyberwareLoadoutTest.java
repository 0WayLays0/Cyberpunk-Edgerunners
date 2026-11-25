package com.waylays.chromeneon.common.cyberware;

import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static com.waylays.chromeneon.common.cyberware.TestCatalog.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The body's rules: where an implant fits, what it costs, what excludes what, and what removal gives back. */
class CyberwareLoadoutTest {
    private static final TestCatalog CATALOG = new TestCatalog();
    private static final int HUMANITY = 100, CAPACITY = 24;

    private static InstallResult install(CyberwareLoadout l, ResourceLocation implant, ResourceLocation slot) {
        return l.install(implant, slot, CATALOG, HUMANITY, CAPACITY);
    }

    @Test
    void installsIntoItsSlotAndCostsHumanityAndCapacity() {
        CyberwareLoadout l = new CyberwareLoadout();
        assertEquals(InstallResult.OK, install(l, DECK, SYSTEM));
        assertEquals(1, l.in(SYSTEM).size());
        assertEquals(DECK, l.in(SYSTEM).getFirst().id());
        assertEquals(20, l.humanityCost(CATALOG));
        assertEquals(6, l.capacityUsed(CATALOG));
        assertTrue(l.in(SYSTEM).getFirst().state().enabled, "a new implant starts switched on");
    }

    @Test
    void refusesUnknownIdsAndWrongSlots() {
        CyberwareLoadout l = new CyberwareLoadout();
        assertEquals(InstallResult.UNKNOWN_IMPLANT, install(l, id("nope"), SYSTEM));
        assertEquals(InstallResult.UNKNOWN_SLOT, install(l, EYES, id("tail")));
        assertEquals(InstallResult.WRONG_SLOT, install(l, EYES, SYSTEM));
        assertTrue(l.entries().isEmpty(), "refused installs change nothing");
    }

    @Test
    void socketsLimitEachSlotAndCopiesAreLimitedPerBody() {
        CyberwareLoadout l = new CyberwareLoadout();
        assertEquals(InstallResult.OK, install(l, EYES, OPTICS));
        assertEquals(InstallResult.ALREADY_INSTALLED, install(l, EYES, OPTICS));
        assertEquals(InstallResult.SLOT_FULL, install(l, EYES2, OPTICS));
        // Blades: one set per arm, two per body
        assertEquals(InstallResult.OK, install(l, BLADES, ARM_L));
        assertEquals(InstallResult.SLOT_FULL, install(l, BLADES, ARM_L));
        assertEquals(InstallResult.OK, install(l, BLADES, ARM_R));
        assertEquals(2, l.count(BLADES));
    }

    @Test
    void oneOperatingSystemPerBody() {
        CyberwareLoadout l = new CyberwareLoadout();
        install(l, CHIP, BIOCHIP);
        assertEquals(InstallResult.OK, install(l, ACCEL, SYSTEM));
        // The system slot has a single socket: the second system cannot go in
        assertEquals(InstallResult.SLOT_FULL, install(l, DECK, SYSTEM));
        CyberwareLoadout other = new CyberwareLoadout();
        assertEquals(InstallResult.OK, install(other, DECK, SYSTEM));
        other.remove(SYSTEM, 0);
        assertEquals(InstallResult.OK, install(other, ACCEL, SYSTEM), "the socket and the group are free again");
    }

    @Test
    void exclusiveGroupsConflictAcrossSlots() {
        TestCatalog catalog = new TestCatalog() {
            @Override
            public java.util.Optional<BodySlot> slot(ResourceLocation id) {
                return id.equals(SYSTEM) ? java.util.Optional.of(new BodySlot(2, BodySlot.Region.SPINE)) : super.slot(id);
            }
        };
        CyberwareLoadout l = new CyberwareLoadout();
        assertEquals(InstallResult.OK, l.install(DECK, SYSTEM, catalog, HUMANITY, CAPACITY));
        assertEquals(InstallResult.CONFLICT, l.install(ACCEL, SYSTEM, catalog, HUMANITY, CAPACITY));
    }

    @Test
    void humanityAndCapacityAreHardLimits() {
        CyberwareLoadout l = new CyberwareLoadout();
        assertEquals(InstallResult.OK, install(l, ACCEL, SYSTEM)); // 25 humanity, 8 capacity
        assertEquals(InstallResult.NOT_ENOUGH_HUMANITY, l.install(HEAVY, SKIN, CATALOG, 84, CAPACITY), "25 + 60 > 84");
        assertEquals(InstallResult.OK, l.install(HEAVY, SKIN, CATALOG, 85, CAPACITY), "exactly at the limit is allowed");
        CyberwareLoadout tight = new CyberwareLoadout();
        assertEquals(InstallResult.OK, tight.install(ACCEL, SYSTEM, CATALOG, HUMANITY, 12));
        assertEquals(InstallResult.NOT_ENOUGH_CAPACITY, tight.install(PLATE, SKIN, CATALOG, HUMANITY, 12), "8 + 6 > 12");
        assertFalse(tight.contains(PLATE));
    }

    @Test
    void removalGivesTheImplantBackAndFreesItsCosts() {
        CyberwareLoadout l = new CyberwareLoadout();
        install(l, PLATE, SKIN);
        install(l, HEAVY, SKIN);
        var removed = l.remove(SKIN, 0);
        assertTrue(removed.isPresent());
        assertEquals(PLATE, removed.get().id());
        assertEquals(HEAVY, l.in(SKIN).getFirst().id(), "the other socket keeps its implant");
        assertEquals(60, l.humanityCost(CATALOG));
        assertTrue(l.remove(SKIN, 5).isEmpty());
        assertTrue(l.remove(OPTICS, 0).isEmpty());
    }

    @Test
    void ramBonusCountsOnlyWorkingImplants() {
        CyberwareLoadout l = new CyberwareLoadout();
        install(l, DECK, SYSTEM);
        install(l, CHIP, BIOCHIP);
        assertEquals(6, l.ramBonus(CATALOG, 100));
        l.in(BIOCHIP).getFirst().state().enabled = false;
        assertEquals(4, l.ramBonus(CATALOG, 100), "switched off: no bonus");
        l.in(SYSTEM).getFirst().state().malfunctionUntil = 200;
        assertEquals(0, l.ramBonus(CATALOG, 100), "malfunctioning: no bonus");
        assertEquals(4, l.ramBonus(CATALOG, 200), "back once the malfunction ends");
    }

    @Test
    void unknownImplantsStayListedButCostNothing() {
        CyberwareLoadout l = new CyberwareLoadout();
        install(l, PLATE, SKIN);
        CyberwareCatalog without = new TestCatalog() {
            @Override
            public java.util.Optional<Implant> implant(ResourceLocation id) {
                return id.equals(PLATE) ? java.util.Optional.empty() : super.implant(id);
            }
        };
        assertTrue(l.contains(PLATE), "kept: the mod may come back");
        assertEquals(0, l.humanityCost(without));
        assertEquals(0, l.capacityUsed(without));
    }

    @Test
    void codecKeepsSlotsOrderAndStates() {
        CyberwareLoadout l = new CyberwareLoadout();
        install(l, BLADES, ARM_L);
        install(l, PLATE, SKIN);
        install(l, HEAVY, SKIN);
        ImplantState s = l.in(SKIN).get(1).state();
        s.enabled = false;
        s.active = true;
        s.activeUntil = 40;
        s.cooldownUntil = 90;
        s.charges = 2;
        s.resource = 12.5F;
        s.malfunctionUntil = 300;
        s.changedAt = 7;
        var json = CyberwareLoadout.CODEC.encodeStart(JsonOps.INSTANCE, l).getOrThrow();
        CyberwareLoadout back = CyberwareLoadout.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(PLATE, back.in(SKIN).get(0).id());
        assertEquals(HEAVY, back.in(SKIN).get(1).id());
        ImplantState b = back.in(SKIN).get(1).state();
        assertFalse(b.enabled);
        assertTrue(b.active);
        assertEquals(40, b.activeUntil);
        assertEquals(90, b.cooldownUntil);
        assertEquals(2, b.charges);
        assertEquals(12.5F, b.resource);
        assertEquals(300, b.malfunctionUntil);
        assertEquals(7, b.changedAt);
        assertEquals(1, back.in(ARM_L).size());
    }

    @Test
    void copiesAreIndependent() {
        CyberwareLoadout l = new CyberwareLoadout();
        install(l, EYES, OPTICS);
        CyberwareLoadout copy = l.copy();
        copy.in(OPTICS).getFirst().state().active = true;
        copy.remove(OPTICS, 0);
        assertFalse(l.in(OPTICS).getFirst().state().active);
        assertTrue(l.contains(EYES));
    }

    @Test
    void stateRules() {
        ImplantState s = new ImplantState();
        assertTrue(s.working(0));
        s.malfunctionUntil = 50;
        assertFalse(s.working(49));
        assertTrue(s.working(50));
        s.cooldownUntil = 10;
        assertTrue(s.coolingDown(9));
        assertFalse(s.coolingDown(10));
        s.setActive(true, 5);
        assertEquals(5, s.changedAt);
        s.setActive(true, 8);
        assertEquals(5, s.changedAt, "no change, no new timestamp");
    }

    @Test
    void propertiesRejectNonsense() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Implant.Properties.of(Implant.Mode.PASSIVE, -1, 0, SKIN));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Implant.Properties.of(Implant.Mode.PASSIVE, 1, 1));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new BodySlot(0, BodySlot.Region.SKIN));
        assertTrue(new TestCatalog().implant(ACCEL).orElseThrow().operatingSystem());
        assertFalse(new TestCatalog().implant(EYES).orElseThrow().operatingSystem());
    }
}
