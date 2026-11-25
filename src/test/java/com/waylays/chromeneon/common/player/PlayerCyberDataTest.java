package com.waylays.chromeneon.common.player;

import com.mojang.serialization.JsonOps;
import com.waylays.chromeneon.common.cyberware.BodySlot;
import com.waylays.chromeneon.common.cyberware.Implant;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCyberDataTest {
    private static final ResourceLocation DECK = ResourceLocation.fromNamespaceAndPath("test", "deck");
    private static final Function<ResourceLocation, Optional<Implant>> LOOKUP = rl -> rl.equals(DECK)
            ? Optional.of(new Implant(BodySlot.SYSTEM, 15, 4, Optional.empty())) : Optional.empty();

    @Test
    void disabledImplantsKeepTheirHumanityCostButGiveNoRam() {
        PlayerCyberData data = new PlayerCyberData();
        data.loadout().install(DECK, LOOKUP, 100);
        assertEquals(new CyberStats(85, 100, 0, 8), CyberStats.of(data, LOOKUP, 100, 4, 16));
        assertTrue(data.setEnabled(DECK, false));
        assertEquals(new CyberStats(85, 100, 0, 4), CyberStats.of(data, LOOKUP, 100, 4, 16));
        assertEquals(6, CyberStats.ramCapacity(new PlayerCyberData(), LOOKUP, 10, 6), "capped");
    }

    @Test
    void codecRoundTripKeepsEverything() {
        PlayerCyberData data = new PlayerCyberData();
        data.loadout().install(DECK, LOOKUP, 100);
        data.setEnabled(DECK, false);
        data.ram().set(3, 8);
        var json = PlayerCyberData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        PlayerCyberData back = PlayerCyberData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertTrue(back.loadout().contains(DECK));
        assertFalse(back.isEnabled(DECK));
        assertEquals(3, back.ram().cells());
        assertTrue(back.uninstall(DECK));
        assertTrue(back.disabled().isEmpty(), "uninstalling forgets the switch");
    }

    @Test
    void emptySaveLoadsDefaults() {
        PlayerCyberData data = PlayerCyberData.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).getOrThrow();
        assertTrue(data.loadout().all().isEmpty());
        assertEquals(0, data.ram().cells());
    }
}
