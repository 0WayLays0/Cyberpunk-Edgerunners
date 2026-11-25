package com.waylays.chromeneon.common.player;

import com.mojang.serialization.JsonOps;
import com.waylays.chromeneon.common.cyberware.InstallResult;
import com.waylays.chromeneon.common.cyberware.TestCatalog;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static com.waylays.chromeneon.common.cyberware.TestCatalog.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the player's sheet derives from the implants (humanity, RAM, capacity) and how it is saved and sent. */
class PlayerCyberDataTest {
    private static final TestCatalog CATALOG = new TestCatalog();
    private static final CyberStats.Limits LIMITS = new CyberStats.Limits(100, 4, 10, 60, 24);

    @Test
    void statsComeFromTheImplants() {
        PlayerCyberData data = new PlayerCyberData();
        assertEquals(InstallResult.OK, data.loadout().install(DECK, SYSTEM, CATALOG, 100, 24));
        assertEquals(InstallResult.OK, data.loadout().install(CHIP, BIOCHIP, CATALOG, 100, 24));
        data.ram().set(3, 10);
        CyberStats s = CyberStats.of(data, CATALOG, LIMITS, 50);
        assertEquals(72, s.humanity(), "100 - 20 - 8");
        assertEquals(100, s.maxHumanity());
        assertEquals(3, s.ram());
        assertEquals(10, s.ramCapacity(), "4 base + 4 + 2, capped at 10");
        assertEquals(10, s.capacityUsed());
        assertEquals(24, s.capacity());
        assertEquals(50, s.gameTime());
        assertEquals(0.72F, s.humanityRatio(), 1e-6);
    }

    @Test
    void switchedOffImplantsKeepTheirHumanityCostButGiveNoRam() {
        PlayerCyberData data = new PlayerCyberData();
        data.loadout().install(DECK, SYSTEM, CATALOG, 100, 24);
        data.loadout().in(SYSTEM).getFirst().state().enabled = false;
        CyberStats s = CyberStats.of(data, CATALOG, LIMITS, 0);
        assertEquals(80, s.humanity(), "the body still carries it");
        assertEquals(4, s.ramCapacity());
    }

    @Test
    void capacityGrowsWithMatricesUpToTheMaximum() {
        PlayerCyberData data = new PlayerCyberData();
        assertTrue(data.addCapacity(2, 3));
        assertTrue(data.addCapacity(2, 3));
        assertEquals(3, data.capacityBonus(), "clamped");
        assertFalse(data.addCapacity(2, 3), "full: nothing consumed");
        assertEquals(27, CyberStats.capacity(data, LIMITS));
        data.setCapacityBonus(-5);
        assertEquals(0, data.capacityBonus());
    }

    @Test
    void codecRoundTripKeepsEverythingSaved() {
        PlayerCyberData data = new PlayerCyberData();
        data.loadout().install(BLADES, ARM_R, CATALOG, 100, 24);
        data.loadout().in(ARM_R).getFirst().state().cooldownUntil = 1234;
        data.ram().set(3, 8);
        data.addCapacity(4, 16);
        data.lastHurtAt = 99;
        var json = PlayerCyberData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        PlayerCyberData back = PlayerCyberData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertTrue(back.loadout().contains(BLADES));
        assertEquals(1234, back.loadout().in(ARM_R).getFirst().state().cooldownUntil);
        assertEquals(3, back.ram().cells());
        assertEquals(4, back.capacityBonus());
        assertEquals(-10_000L, back.lastHurtAt, "transient values are not saved");
    }

    @Test
    void emptySaveLoadsDefaults() {
        PlayerCyberData data = PlayerCyberData.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).getOrThrow();
        assertTrue(data.loadout().entries().isEmpty());
        assertEquals(0, data.ram().cells());
        assertEquals(0, data.capacityBonus());
    }

    @Test
    void statsSurviveTheNetwork() {
        CyberStats s = new CyberStats(72, 100, 3, 10, 25, 60, 10, 24, 123_456_789L);
        var buf = Unpooled.buffer();
        CyberStats.STREAM_CODEC.encode(buf, s);
        assertEquals(s, CyberStats.STREAM_CODEC.decode(buf));
    }
}
