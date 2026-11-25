package com.waylays.chromeneon.common.cyberware;

import java.util.Locale;

/** Why a cyberware operation can or cannot be done. Each refusal has its own message in the interfaces. */
public enum InstallResult {
    OK,
    UNKNOWN_IMPLANT,
    UNKNOWN_SLOT,
    WRONG_SLOT,
    SLOT_FULL,
    ALREADY_INSTALLED,
    CONFLICT,
    NOT_ENOUGH_HUMANITY,
    NOT_ENOUGH_CAPACITY,
    NOT_INSTALLED,
    NOT_SEATED,
    IN_COMBAT,
    NO_ITEM,
    INVENTORY_FULL;

    public boolean ok() {
        return this == OK;
    }

    public String translationKey() {
        return "chromeneon.install." + name().toLowerCase(Locale.ROOT);
    }
}
