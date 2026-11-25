package com.waylays.chromeneon.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-player display options ({@code config/chromeneon-client.toml}). Plain config values with no client classes,
 * so it is registered from the common entry point; only client code reads it.
 */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.DoubleValue HUD_OPACITY;
    public static final ModConfigSpec.BooleanValue GLITCH_EFFECTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hud");
        HUD_ENABLED = b.comment("Draw the Chrome Néon HUD.").define("enabled", true);
        HUD_OPACITY = b.comment("Opacity of the HUD panels, 0 to 1.").defineInRange("opacity", 0.9, 0.1, 1.0);
        GLITCH_EFFECTS = b.comment("Glitch effects on alerts (never permanent). Off for players sensitive to flicker.")
                .define("glitchEffects", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}
}
