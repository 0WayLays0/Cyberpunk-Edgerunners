package com.waylays.chromeneon.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-player display options ({@code config/chromeneon-client.toml}), editable in game (Mods > Chrome Néon >
 * Config). Plain config values with no client classes, so it is registered from the common entry point; only client
 * code reads it. Every value is read live: changes apply without restarting.
 */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.DoubleValue HUD_OPACITY;
    public static final ModConfigSpec.BooleanValue MINIMAL;
    public static final ModConfigSpec.DoubleValue FX_INTENSITY;
    public static final ModConfigSpec.BooleanValue GLITCH_EFFECTS;
    public static final ModConfigSpec.BooleanValue BOOT_SEQUENCE;

    public static final Element VITALS;
    public static final Element STATUS;
    public static final Element NAVIGATION;
    public static final Element NOTIFICATIONS;
    public static final Element CALL;

    public static final ModConfigSpec.BooleanValue CLOCK_ENABLED;
    public static final ModConfigSpec.BooleanValue MINIMAP_ENABLED;
    public static final ModConfigSpec.IntValue MINIMAP_SIZE;
    public static final ModConfigSpec.DoubleValue MINIMAP_ZOOM;
    public static final ModConfigSpec.BooleanValue MINIMAP_ROTATE;
    public static final ModConfigSpec.BooleanValue MINIMAP_ENTITIES;
    public static final ModConfigSpec.BooleanValue MINIMAP_MARKERS;

    public static final ModConfigSpec.IntValue NOTIFICATIONS_MAX;
    public static final ModConfigSpec.BooleanValue TARGET_BRACKETS;
    public static final ModConfigSpec.IntValue TARGET_RANGE;

    /** Enabled flag, anchor and offset of one HUD group. */
    public record Element(ModConfigSpec.BooleanValue enabled, ModConfigSpec.EnumValue<HudAnchor> anchor,
                          ModConfigSpec.IntValue x, ModConfigSpec.IntValue y) {
        static Element define(ModConfigSpec.Builder b, String name, String comment, HudAnchor anchor, int x, int y) {
            b.comment(comment).push(name);
            Element e = new Element(
                    b.comment("Show this group.").define("enabled", true),
                    b.comment("Screen point the group is attached to.").defineEnum("anchor", anchor),
                    b.comment("Horizontal distance from the anchor, inward, in GUI pixels.").defineInRange("x", x, -2000, 2000),
                    b.comment("Vertical distance from the anchor, inward, in GUI pixels.").defineInRange("y", y, -2000, 2000));
            b.pop();
            return e;
        }
    }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hud");
        HUD_ENABLED = b.comment("Draw the Chrome Néon HUD (the vanilla health and armor come back when off).")
                .define("enabled", true);
        HUD_SCALE = b.comment("Size of the HUD relative to the game's GUI scale.").defineInRange("scale", 1.0, 0.5, 2.0);
        HUD_OPACITY = b.comment("Opacity of the HUD, 0.1 to 1.").defineInRange("opacity", 0.95, 0.1, 1.0);
        MINIMAL = b.comment("Minimal mode: essential data only, no decorative labels or details.").define("minimal", false);
        FX_INTENSITY = b.comment("Strength of screen effects (damage, low health), 0 turns them off.")
                .defineInRange("effectsIntensity", 1.0, 0.0, 1.0);
        GLITCH_EFFECTS = b.comment("Glitch on strong events (heavy damage, alerts). Never permanent.",
                "Turn off for players sensitive to flicker.").define("glitchEffects", true);
        BOOT_SEQUENCE = b.comment("Animate the HUD powering on when joining a world.").define("bootSequence", true);
        b.pop();

        VITALS = Element.define(b, "vitals", "Health, armor and RAM.", HudAnchor.TOP_LEFT, 8, 8);
        STATUS = Element.define(b, "status", "Active effects and abnormal states.", HudAnchor.TOP_LEFT, 8, 52);
        NAVIGATION = Element.define(b, "navigation", "Minimap and clock.", HudAnchor.TOP_RIGHT, 8, 8);
        NOTIFICATIONS = Element.define(b, "notifications", "System notifications.", HudAnchor.TOP_LEFT, 8, 96);
        CALL = Element.define(b, "call", "Incoming call popup (under the navigation group by default).", HudAnchor.TOP_RIGHT, 8, 131);

        b.push("minimap");
        CLOCK_ENABLED = b.comment("Show the clock under the minimap.").define("clock", true);
        MINIMAP_ENABLED = b.comment("Show the minimap.").define("enabled", true);
        MINIMAP_SIZE = b.comment("Side of the minimap, in GUI pixels.").defineInRange("size", 72, 48, 128);
        MINIMAP_ZOOM = b.comment("Blocks per minimap pixel (lower is closer).").defineInRange("zoom", 1.0, 0.5, 4.0);
        MINIMAP_ROTATE = b.comment("Rotate the map with the player (false: north stays up).").define("rotate", true);
        MINIMAP_ENTITIES = b.comment("Show nearby creatures and players.").define("entities", true);
        MINIMAP_MARKERS = b.comment("Show markers, world spawn and the last death point.").define("markers", true);
        b.pop();

        b.push("notifications");
        NOTIFICATIONS_MAX = b.comment("Notifications on screen at once; 0 turns them off.").defineInRange("maxVisible", 3, 0, 6);
        b.pop();

        b.push("targeting");
        TARGET_BRACKETS = b.comment("Brackets and name on the creature you look at (scanning works either way).")
                .define("brackets", true);
        TARGET_RANGE = b.comment("Distance, in blocks, at which creatures are targeted.").defineInRange("range", 32, 4, 64);
        b.pop();

        SPEC = b.build();
    }

    private ClientConfig() {}
}
