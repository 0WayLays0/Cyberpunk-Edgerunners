package com.waylays.chromeneon.common.cyberware;

import java.util.Locale;

/** Outcome of asking an implant to act. Refusals are shown to the player with their reason. */
public enum UseResult {
    OK,
    NOT_INSTALLED,
    DISABLED,
    MALFUNCTION,
    COOLDOWN,
    NO_RAM,
    NO_CHARGE,
    NOT_NOW,
    OFFLINE;

    public boolean ok() {
        return this == OK;
    }

    public String translationKey() {
        return "chromeneon.use." + name().toLowerCase(Locale.ROOT);
    }
}
