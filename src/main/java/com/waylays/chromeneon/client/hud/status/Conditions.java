package com.waylays.chromeneon.client.hud.status;

import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * Abnormal states read from the player: burning, freezing, out of air, starving. Each has an icon, a label and,
 * when it is measurable, how far it has gone (0..1).
 */
public final class Conditions {
    /** {@code level} is NaN when the state has no measure. {@code id} keeps the onset animation per state. */
    public record Condition(String id, HudIcon icon, Component label, int color, float level) {}

    private Conditions() {}

    public static List<Condition> of(LocalPlayer p) {
        List<Condition> list = new ArrayList<>();
        if (p.isOnFire() && !p.fireImmune() && !p.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            list.add(new Condition("fire", HudIcon.FIRE, Component.translatable("chromeneon.hud.status.fire"), NeonPalette.RED, Float.NaN));
        }
        if (p.getTicksFrozen() > 0) {
            list.add(new Condition("freeze", HudIcon.FREEZE, Component.translatable("chromeneon.hud.status.freeze"),
                    NeonPalette.CYAN, p.getPercentFrozen()));
        }
        int air = p.getAirSupply(), maxAir = p.getMaxAirSupply();
        if (air < maxAir && (p.isUnderWater() || air < maxAir - 20)) {
            float level = Math.max(0F, air) / (float) maxAir;
            list.add(new Condition("air", HudIcon.AIR, Component.translatable("chromeneon.hud.status.air"),
                    level < 0.3F ? NeonPalette.RED : NeonPalette.CYAN, level));
        }
        int food = p.getFoodData().getFoodLevel();
        if (food <= 6 && !p.getAbilities().invulnerable) {
            list.add(new Condition("food", HudIcon.HUNGER, Component.translatable(food == 0
                    ? "chromeneon.hud.status.starving" : "chromeneon.hud.status.hungry"),
                    food == 0 ? NeonPalette.RED : NeonPalette.ORANGE, food / 20F));
        }
        return list;
    }
}
