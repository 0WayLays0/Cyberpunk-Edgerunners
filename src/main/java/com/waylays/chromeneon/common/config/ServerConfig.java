package com.waylays.chromeneon.common.config;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gameplay balance, per world ({@code serverconfig/chromeneon-server.toml}) and synced to clients by NeoForge. The
 * cyberware logic reads these values when it runs, so a change applies without restarting.
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_HUMANITY, BASE_CAPACITY, CAPACITY_BONUS_MAX, SURGERY_CALM_TICKS;
    public static final ModConfigSpec.BooleanValue MALFUNCTIONS;
    public static final ModConfigSpec.DoubleValue STRAIN_THRESHOLD, MALFUNCTION_CHANCE;
    public static final ModConfigSpec.IntValue MALFUNCTION_TICKS;

    public static final ModConfigSpec.IntValue BASE_RAM, RAM_CAP, RAM_REGEN_TICKS;

    public static final ModConfigSpec.IntValue ACCEL_DURATION, ACCEL_COOLDOWN, ACCEL_RAM, ACCEL_RADIUS, ACCEL_SLOWDOWN;

    public static final ModConfigSpec.DoubleValue BLADE_DAMAGE, BLADE_SPEED, LUNGE_SPEED, LUNGE_DAMAGE, DILATION_BLADE_BONUS;
    public static final ModConfigSpec.IntValue LUNGE_COOLDOWN;

    public static final ModConfigSpec.DoubleValue JUMP_VELOCITY;
    public static final ModConfigSpec.IntValue EXTRA_JUMPS;

    public static final ModConfigSpec.DoubleValue DERMIS_INTEGRITY, DERMIS_SHARE, DERMIS_REGEN;
    public static final ModConfigSpec.IntValue DERMIS_REGEN_DELAY;

    public static final ModConfigSpec.DoubleValue PUMP_TRIGGER, PUMP_HEAL;
    public static final ModConfigSpec.IntValue PUMP_CHARGES, PUMP_RECHARGE, PUMP_COOLDOWN;

    public static final ModConfigSpec.DoubleValue OPTICS_MAX_ZOOM;
    public static final ModConfigSpec.IntValue TACTICAL_DURATION, TACTICAL_COOLDOWN, TACTICAL_RAM, TACTICAL_RANGE;

    public static final ModConfigSpec.DoubleValue COPROCESSOR_REGEN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("cyberware");
        MAX_HUMANITY = b.comment("Humanity of an unmodified body. Every implant costs part of it; an install that",
                "would bring it below zero is refused.").defineInRange("maxHumanity", 100, 10, 1000);
        BASE_CAPACITY = b.comment("Cyberware capacity of a body: the sum of the implants' capacity costs cannot exceed it.")
                .defineInRange("baseCapacity", 24, 1, 500);
        CAPACITY_BONUS_MAX = b.comment("Capacity a player can add with neural matrices, at most.")
                .defineInRange("capacityBonusMax", 16, 0, 500);
        SURGERY_CALM_TICKS = b.comment("Ticks without taking damage before surgery is allowed.")
                .defineInRange("surgeryCalmTicks", 100, 0, 6000);
        b.pop();

        b.push("humanity");
        MALFUNCTIONS = b.comment("Below the strain threshold, implants can malfunction for a while (never permanently).")
                .define("malfunctions", true);
        STRAIN_THRESHOLD = b.comment("Share of max humanity under which the body is strained.").defineInRange("strainThreshold", 0.25, 0.0, 1.0);
        MALFUNCTION_CHANCE = b.comment("Chance per minute, per implant, of a malfunction at zero humanity (scaled down",
                "linearly to 0 at the threshold).").defineInRange("malfunctionChancePerMinute", 0.5, 0.0, 1.0);
        MALFUNCTION_TICKS = b.comment("How long a malfunction lasts, in ticks.").defineInRange("malfunctionTicks", 200, 20, 6000);
        b.pop();

        b.push("ram");
        BASE_RAM = b.comment("RAM cells available without any implant raising it.").defineInRange("baseCells", 4, 0, 32);
        RAM_CAP = b.comment("Hard limit on RAM cells, whatever the implants add.").defineInRange("maxCells", 16, 1, 64);
        RAM_REGEN_TICKS = b.comment("Ticks needed to recover one spent RAM cell (20 ticks = 1 second).")
                .defineInRange("regenTicksPerCell", 60, 1, 72000);
        b.pop();

        b.push("chronos_accelerator");
        ACCEL_DURATION = b.comment("Ticks the time dilation lasts.").defineInRange("durationTicks", 120, 10, 2400);
        ACCEL_COOLDOWN = b.comment("Ticks before it can run again, counted from its end.").defineInRange("cooldownTicks", 500, 0, 24000);
        ACCEL_RAM = b.comment("RAM cells spent to start it.").defineInRange("ramCost", 2, 0, 16);
        ACCEL_RADIUS = b.comment("Radius, in blocks, of the slowed zone around the player.").defineInRange("radius", 24, 4, 64);
        ACCEL_SLOWDOWN = b.comment("Inside the zone, other entities live one tick out of this many.").defineInRange("slowdown", 4, 2, 10);
        b.pop();

        b.push("reaper_blades");
        BLADE_DAMAGE = b.comment("Attack damage added per deployed blade (empty hand).").defineInRange("damagePerBlade", 5.0, 0.0, 50.0);
        BLADE_SPEED = b.comment("Attack speed added while blades are deployed.").defineInRange("attackSpeed", 0.5, 0.0, 4.0);
        LUNGE_COOLDOWN = b.comment("Ticks between two lunges.").defineInRange("lungeCooldownTicks", 50, 0, 1200);
        LUNGE_SPEED = b.comment("Lunge speed, in blocks per tick.").defineInRange("lungeSpeed", 1.4, 0.2, 4.0);
        LUNGE_DAMAGE = b.comment("Damage of a lunge hit.").defineInRange("lungeDamage", 8.0, 0.0, 100.0);
        DILATION_BLADE_BONUS = b.comment("Extra share of blade damage while time is dilated.").defineInRange("dilationBonus", 0.3, 0.0, 5.0);
        b.pop();

        b.push("spring_tendons");
        JUMP_VELOCITY = b.comment("Upward speed of the extra jump.").defineInRange("jumpVelocity", 0.62, 0.1, 2.0);
        EXTRA_JUMPS = b.comment("Jumps allowed in the air before landing (one more while time is dilated).")
                .defineInRange("extraJumps", 1, 1, 5);
        b.pop();

        b.push("carapace_dermis");
        DERMIS_INTEGRITY = b.comment("Damage the plating can absorb before it is breached.").defineInRange("integrity", 20.0, 1.0, 500.0);
        DERMIS_SHARE = b.comment("Share of each physical hit taken by the plating.").defineInRange("absorbShare", 0.4, 0.0, 1.0);
        DERMIS_REGEN_DELAY = b.comment("Ticks without damage before the plating repairs itself.").defineInRange("regenDelayTicks", 80, 0, 6000);
        DERMIS_REGEN = b.comment("Integrity repaired per second.").defineInRange("regenPerSecond", 1.0, 0.0, 100.0);
        b.pop();

        b.push("hemostat_pump");
        PUMP_TRIGGER = b.comment("Health share under which the pump injects.").defineInRange("triggerHealthRatio", 0.3, 0.05, 0.9);
        PUMP_HEAL = b.comment("Health restored by an injection (half at once, half over two seconds).").defineInRange("heal", 8.0, 1.0, 100.0);
        PUMP_CHARGES = b.comment("Injections stored.").defineInRange("charges", 2, 1, 10);
        PUMP_RECHARGE = b.comment("Ticks to refill one injection.").defineInRange("rechargeTicks", 1200, 20, 72000);
        PUMP_COOLDOWN = b.comment("Ticks between two injections.").defineInRange("cooldownTicks", 200, 0, 6000);
        b.pop();

        b.push("argus_optics");
        OPTICS_MAX_ZOOM = b.comment("Strongest zoom.").defineInRange("maxZoom", 4.0, 1.5, 10.0);
        TACTICAL_DURATION = b.comment("Ticks a tactical analysis lasts.").defineInRange("tacticalDurationTicks", 300, 20, 6000);
        TACTICAL_COOLDOWN = b.comment("Ticks before another analysis, from its end.").defineInRange("tacticalCooldownTicks", 200, 0, 6000);
        TACTICAL_RAM = b.comment("RAM cells spent to start an analysis.").defineInRange("tacticalRamCost", 1, 0, 16);
        TACTICAL_RANGE = b.comment("Range of the analysis, in blocks.").defineInRange("tacticalRange", 40, 8, 128);
        b.pop();

        b.push("synapse_coprocessor");
        COPROCESSOR_REGEN = b.comment("RAM regeneration time multiplier while installed (0.7 = 30% faster).")
                .defineInRange("regenMultiplier", 0.7, 0.1, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private ServerConfig() {}

    /** Reach used by the in-combat check: a player hurt within this many ticks cannot undergo surgery. */
    public static boolean inCombat(Player player, long lastHurtAt) {
        return player.level().getGameTime() - lastHurtAt < SURGERY_CALM_TICKS.get();
    }
}
