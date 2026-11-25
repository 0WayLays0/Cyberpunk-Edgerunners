package com.waylays.chromeneon.client.hud.status;

import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import java.util.Map;

/**
 * Maps status effects to the HUD's own icons. Vanilla effects have a dedicated icon; effects from other mods get
 * the generic icon of their category, so every effect is shown in the same graphic language.
 */
public final class EffectIcons {
    private static final Map<String, HudIcon> VANILLA = Map.ofEntries(
            Map.entry("speed", HudIcon.SPEED), Map.entry("slowness", HudIcon.SLOW),
            Map.entry("haste", HudIcon.HASTE), Map.entry("mining_fatigue", HudIcon.FATIGUE),
            Map.entry("strength", HudIcon.STRENGTH), Map.entry("instant_health", HudIcon.REGEN),
            Map.entry("instant_damage", HudIcon.BAD), Map.entry("jump_boost", HudIcon.JUMP),
            Map.entry("nausea", HudIcon.NAUSEA), Map.entry("regeneration", HudIcon.REGEN),
            Map.entry("resistance", HudIcon.SHIELD), Map.entry("fire_resistance", HudIcon.FIRE_SHIELD),
            Map.entry("water_breathing", HudIcon.WATER), Map.entry("invisibility", HudIcon.GHOST),
            Map.entry("blindness", HudIcon.EYE_OFF), Map.entry("night_vision", HudIcon.EYE),
            Map.entry("hunger", HudIcon.HUNGER), Map.entry("weakness", HudIcon.WEAKNESS),
            Map.entry("poison", HudIcon.POISON), Map.entry("wither", HudIcon.WITHER),
            Map.entry("health_boost", HudIcon.HEART), Map.entry("absorption", HudIcon.ABSORB),
            Map.entry("saturation", HudIcon.HUNGER), Map.entry("glowing", HudIcon.GLOW),
            Map.entry("levitation", HudIcon.LEVITATE), Map.entry("luck", HudIcon.LUCK),
            Map.entry("unluck", HudIcon.LUCK), Map.entry("slow_falling", HudIcon.FEATHER),
            Map.entry("conduit_power", HudIcon.WATER), Map.entry("dolphins_grace", HudIcon.WATER),
            Map.entry("bad_omen", HudIcon.FLAG), Map.entry("hero_of_the_village", HudIcon.FLAG),
            Map.entry("darkness", HudIcon.EYE_OFF), Map.entry("trial_omen", HudIcon.FLAG),
            Map.entry("raid_omen", HudIcon.FLAG), Map.entry("wind_charged", HudIcon.LEVITATE),
            Map.entry("weaving", HudIcon.BAD), Map.entry("oozing", HudIcon.POISON), Map.entry("infested", HudIcon.BAD));

    private EffectIcons() {}

    public static HudIcon icon(Holder<MobEffect> effect) {
        ResourceLocation key = effect.unwrapKey().map(k -> k.location()).orElse(null);
        if (key != null && key.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            HudIcon icon = VANILLA.get(key.getPath());
            if (icon != null) return icon;
        }
        return effect.value().getCategory() == MobEffectCategory.HARMFUL ? HudIcon.BAD : HudIcon.GOOD;
    }

    /** Cyan for helpful effects, orange for harmful ones, violet for neutral ones. */
    public static int color(Holder<MobEffect> effect) {
        return switch (effect.value().getCategory()) {
            case BENEFICIAL -> NeonPalette.CYAN;
            case HARMFUL -> NeonPalette.ORANGE;
            case NEUTRAL -> NeonPalette.VIOLET_HOT;
        };
    }
}
