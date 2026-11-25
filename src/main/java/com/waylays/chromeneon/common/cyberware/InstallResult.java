package com.waylays.chromeneon.common.cyberware;

/** Why an implant can or cannot be installed. Each refusal has its own message for the ripperdoc interface. */
public enum InstallResult {
    OK,
    UNKNOWN_IMPLANT,
    ALREADY_INSTALLED,
    SLOT_FULL,
    CONFLICT,
    NOT_ENOUGH_HUMANITY;

    public boolean ok() {
        return this == OK;
    }

    public String translationKey() {
        return "chromeneon.install." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
