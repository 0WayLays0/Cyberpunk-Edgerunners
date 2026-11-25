package com.waylays.chromeneon.common.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.waylays.chromeneon.common.cyberware.CyberwareLoadout;
import com.waylays.chromeneon.common.hacking.RamPool;

/**
 * Everything persistent about a player's cyberware, attached to the player ({@code chromeneon:cyber_data}): the
 * installed implants with their states, the RAM, and the capacity gained through play. Humanity is not stored: it
 * is derived from the implants, so it can never drift from them. Fields have defaults so old saves keep loading.
 * A few transient values (last damage, dash in progress) live here too and are not saved.
 */
public final class PlayerCyberData {
    public static final Codec<PlayerCyberData> CODEC = RecordCodecBuilder.create(i -> i.group(
            CyberwareLoadout.CODEC.optionalFieldOf("loadout", new CyberwareLoadout()).forGetter(d -> d.loadout),
            Codec.INT.optionalFieldOf("ram", 0).forGetter(d -> d.ram.cells()),
            Codec.INT.optionalFieldOf("capacity_bonus", 0).forGetter(d -> d.capacityBonus)
    ).apply(i, PlayerCyberData::new));

    private final CyberwareLoadout loadout;
    private final RamPool ram;
    private int capacityBonus;

    /** Server tick of the last damage taken (not saved): no surgery in the middle of a fight. */
    public long lastHurtAt = -10_000L;
    /** Ticks left of a blade lunge and the entities it already hit (not saved). */
    public int lungeTicks;
    public final java.util.Set<Integer> lungeHits = new java.util.HashSet<>();

    public PlayerCyberData() {
        this(new CyberwareLoadout(), 0, 0);
    }

    private PlayerCyberData(CyberwareLoadout loadout, int ram, int capacityBonus) {
        this.loadout = loadout.copy();
        this.ram = new RamPool(ram);
        this.capacityBonus = Math.max(0, capacityBonus);
    }

    public CyberwareLoadout loadout() {
        return loadout;
    }

    public RamPool ram() {
        return ram;
    }

    public int capacityBonus() {
        return capacityBonus;
    }

    public void setCapacityBonus(int bonus) {
        capacityBonus = Math.max(0, bonus);
    }

    /** Raises the capacity bonus, never above {@code max}; returns whether it changed. */
    public boolean addCapacity(int amount, int max) {
        int next = Math.min(max, capacityBonus + amount);
        if (next == capacityBonus) return false;
        capacityBonus = next;
        return true;
    }
}
