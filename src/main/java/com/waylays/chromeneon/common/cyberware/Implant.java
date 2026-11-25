package com.waylays.chromeneon.common.cyberware;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A kind of implant, registered in {@code chromeneon:implant}. The common rules (where it fits, what it costs,
 * what it conflicts with) come from its {@link Properties}; its own mechanics come from the hooks, which each
 * implant overrides as it needs. Hooks run on the server only.
 */
public abstract class Implant {
    /** How the player uses it. */
    public enum Mode {
        /** Always working while enabled (armor plating, coprocessor). */
        PASSIVE,
        /** Switched on and off by the player (blades, optics). */
        TOGGLE,
        /** Triggered for a duration, then cools down (accelerator). */
        TRIGGERED,
        /** Fires by itself when its conditions are met (pump, tendons). */
        AUTOMATIC
    }

    /**
     * @param slots          the body slots it can go in
     * @param maxPerBody     how many can be installed at once (two blade sets: one per arm)
     * @param exclusiveGroup implants of a group exclude each other (one operating system)
     */
    public record Properties(Set<ResourceLocation> slots, int humanityCost, int capacityCost, int ramBonus, Mode mode,
                             int maxPerBody, Optional<String> exclusiveGroup) {
        public Properties {
            if (slots.isEmpty()) throw new IllegalArgumentException("an implant must fit at least one slot");
            if (humanityCost < 0 || capacityCost < 0 || ramBonus < 0 || maxPerBody < 1) {
                throw new IllegalArgumentException("invalid implant costs");
            }
        }

        public static Properties of(Mode mode, int humanity, int capacity, ResourceLocation... slots) {
            return new Properties(Set.of(slots), humanity, capacity, 0, mode, 1, Optional.empty());
        }

        public Properties ram(int bonus) {
            return new Properties(slots, humanityCost, capacityCost, bonus, mode, maxPerBody, exclusiveGroup);
        }

        public Properties perBody(int max) {
            return new Properties(slots, humanityCost, capacityCost, ramBonus, mode, max, exclusiveGroup);
        }

        public Properties exclusive(String group) {
            return new Properties(slots, humanityCost, capacityCost, ramBonus, mode, maxPerBody, Optional.of(group));
        }
    }

    private final Properties properties;

    protected Implant(Properties properties) {
        this.properties = properties;
    }

    public final Properties properties() {
        return properties;
    }

    /** RAM cells it adds while working. */
    public int ramBonus() {
        return properties.ramBonus;
    }

    /** Lines of technical description for the interfaces (mechanics and numbers, from the current config). */
    public abstract List<Component> describe();

    /** Called once when installed; sets the starting state (charges, gauges). */
    public void onInstalled(ImplantContext ctx) {}

    /** Called when removed or when the player loses it; must undo anything still applied. */
    public void onRemoved(ImplantContext ctx) {}

    /** Called every server tick while installed (working or not: an implant may need to recharge or clean up). */
    public void tick(ImplantContext ctx) {}

    /**
     * The player asked to use it (key or interface). Returns why it could not, or OK. The default switches TOGGLE
     * implants; TRIGGERED implants override it; others cannot be used by hand.
     */
    public UseResult use(ImplantContext ctx) {
        if (properties.mode != Mode.TOGGLE) return UseResult.NOT_NOW;
        UseResult ready = ready(ctx);
        if (!ready.ok()) return ready;
        ctx.state().setActive(!ctx.state().active, ctx.now());
        return UseResult.OK;
    }

    /** The common preconditions of any use: switched on, not malfunctioning, the player able to act. */
    protected static UseResult ready(ImplantContext ctx) {
        if (!ctx.state().enabled) return UseResult.DISABLED;
        if (ctx.state().malfunctioning(ctx.now())) return UseResult.MALFUNCTION;
        if (!ctx.player().isAlive() || ctx.player().isSpectator()) return UseResult.NOT_NOW;
        return UseResult.OK;
    }

    /** Called when it stops working (disabled, malfunction, removed): end whatever runs. */
    public void onStopped(ImplantContext ctx) {
        ctx.state().setActive(false, ctx.now());
    }
}
