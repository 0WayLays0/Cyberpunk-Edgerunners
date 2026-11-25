package com.waylays.chromeneon.common.cyberware;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The cybernetic architecture of the body. Each slot is a subsystem with a fixed number of sockets; the logic of
 * each subsystem (time dilation for SYSTEM, scanner for OPTICS, ...) lives with the implants that fill it.
 */
public enum BodySlot implements StringRepresentable {
    /** Operating system: RAM management, time accelerator, core functions. */
    SYSTEM("system", 3),
    /** Optics: scanner, zoom, target analysis. */
    OPTICS("optics", 1),
    /** Arms: blades and integrated tools. */
    ARMS("arms", 1),
    /** Legs: double jump and mobility. */
    LEGS("legs", 1),
    /** Integumentary system: plating and damage handling. */
    INTEGUMENTARY("integumentary", 3),
    /** Circulatory system: biomonitor, recovery, emergency pumps. */
    CIRCULATORY("circulatory", 3),
    /** Biochip: the onboard AI (VESPER) and software abilities. */
    BIOCHIP("biochip", 1);

    public static final Codec<BodySlot> CODEC = StringRepresentable.fromEnum(BodySlot::values);

    private final String name;
    private final int sockets;

    BodySlot(String name, int sockets) {
        this.name = name;
        this.sockets = sockets;
    }

    public int sockets() {
        return sockets;
    }

    public String translationKey() {
        return "chromeneon.slot." + name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
