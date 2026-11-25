package com.waylays.chromeneon.common.cyberware;

import java.util.Optional;

/**
 * A kind of implant, registered in {@code chromeneon:implant}. This base class carries what the install rules need;
 * implants with behaviour (phase 3) extend it and hook into the systems of their slot.
 *
 * @param exclusiveGroup implants sharing a group cannot be installed together (e.g. two operating systems)
 */
public class Implant {
    private final BodySlot slot;
    private final int humanityCost;
    private final int ramBonus;
    private final Optional<String> exclusiveGroup;

    public Implant(BodySlot slot, int humanityCost, int ramBonus, Optional<String> exclusiveGroup) {
        if (humanityCost < 0 || ramBonus < 0) {
            throw new IllegalArgumentException("implant costs and bonuses cannot be negative");
        }
        this.slot = slot;
        this.humanityCost = humanityCost;
        this.ramBonus = ramBonus;
        this.exclusiveGroup = exclusiveGroup;
    }

    public final BodySlot slot() {
        return slot;
    }

    public final int humanityCost() {
        return humanityCost;
    }

    /** RAM cells this implant adds to the player's pool while it is enabled. */
    public final int ramBonus() {
        return ramBonus;
    }

    public final Optional<String> exclusiveGroup() {
        return exclusiveGroup;
    }
}
