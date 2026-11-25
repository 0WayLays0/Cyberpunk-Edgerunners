package com.waylays.chromeneon.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gameplay balance, per world ({@code serverconfig/chromeneon-server.toml}) and synced to clients by NeoForge.
 * Read only through these values: the cyberware and hacking logic takes plain numbers so it stays testable.
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_HUMANITY;
    public static final ModConfigSpec.IntValue BASE_RAM;
    public static final ModConfigSpec.IntValue RAM_CAP;
    public static final ModConfigSpec.IntValue RAM_REGEN_TICKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("cyberware");
        MAX_HUMANITY = b.comment("Humanity of an unmodified body. Every implant costs part of it; an install that",
                        "would bring it below zero is refused.")
                .defineInRange("maxHumanity", 100, 10, 1000);
        b.pop();
        b.push("ram");
        BASE_RAM = b.comment("RAM cells available without any implant raising it.")
                .defineInRange("baseCells", 4, 0, 32);
        RAM_CAP = b.comment("Hard limit on RAM cells, whatever the implants add.")
                .defineInRange("maxCells", 16, 1, 64);
        RAM_REGEN_TICKS = b.comment("Ticks needed to recover one spent RAM cell (20 ticks = 1 second).")
                .defineInRange("regenTicksPerCell", 60, 1, 72000);
        b.pop();
        SPEC = b.build();
    }

    private ServerConfig() {}
}
