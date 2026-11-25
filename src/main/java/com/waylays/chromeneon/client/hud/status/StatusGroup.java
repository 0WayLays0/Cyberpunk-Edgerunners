package com.waylays.chromeneon.client.hud.status;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudGroup;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Status indicators, replacing the vanilla effect list. First the abnormal states, one line each with their name
 * (they matter more); then the effects as icons inside rings that drain with their time, harmful ones first, with
 * their level. No boxes. Minimal mode keeps the states and the harmful effects only.
 */
public final class StatusGroup extends HudGroup {
    private static final float RING = 7.5F, STRIDE = 19F, ROW = 19F, STATE_ROW = 13F;
    private static final int MAX_CHIPS = 10;

    /** Longest duration seen for each running effect, so the line drains from full. */
    private final Map<Holder<MobEffect>, Integer> maxDuration = new HashMap<>();
    /** When each abnormal state started, for its arrival animation. */
    private final Map<String, Float> onset = new HashMap<>();

    public StatusGroup() {
        super("status", ClientConfig.STATUS, 6);
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    private List<MobEffectInstance> effects(HudFrame f) {
        return player().getActiveEffects().stream()
                .filter(MobEffectInstance::showIcon)
                .filter(e -> !f.minimal || e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .sorted(Comparator.comparing((MobEffectInstance e) -> e.getEffect().value().getCategory() != MobEffectCategory.HARMFUL)
                        .thenComparingInt(MobEffectInstance::getDuration))
                .toList();
    }

    @Override
    protected boolean active(HudFrame f) {
        LocalPlayer p = player();
        return p != null && !p.isSpectator() && (!Conditions.of(p).isEmpty() || !effects(f).isEmpty());
    }

    @Override
    protected float width(HudFrame f) {
        float w = 0F;
        for (Conditions.Condition c : Conditions.of(player())) w = Math.max(w, conditionWidth(c));
        return Math.max(w, Math.min(effects(f).size(), MAX_CHIPS) * STRIDE + 20F);
    }

    @Override
    protected float height(HudFrame f) {
        int states = Conditions.of(player()).size();
        boolean effects = !effects(f).isEmpty();
        return states * STATE_ROW + (effects ? ROW : 0F);
    }

    private static float conditionWidth(Conditions.Condition c) {
        return 14F + HudText.width(HudText.title(c.label())) + (Float.isNaN(c.level()) ? 8F : 30F);
    }

    @Override
    protected void draw(GuiGraphics g, HudFrame f) {
        float boot = boot(f);
        List<Conditions.Condition> states = Conditions.of(player());
        onset.keySet().retainAll(states.stream().map(Conditions.Condition::id).toList());
        float y = 0F;
        for (Conditions.Condition c : states) {
            float start = onset.computeIfAbsent(c.id(), k -> f.now);
            drawCondition(g, f, c, y, Easing.progress(f.now, start, 8F) * boot);
            y += STATE_ROW;
        }
        List<MobEffectInstance> effects = effects(f);
        maxDuration.keySet().retainAll(effects.stream().map(MobEffectInstance::getEffect).toList());
        float x = RING;
        for (int i = 0; i < effects.size() && i < MAX_CHIPS; i++) {
            drawEffect(g, f, effects.get(i), x, y + RING, boot);
            x += STRIDE;
        }
        if (effects.size() > MAX_CHIPS) {
            HudText.draw(g, HudText.data("+" + (effects.size() - MAX_CHIPS)), x - RING + 1, y + RING - 2, f.c(NeonPalette.LAVENDER), f.px);
        }
    }

    /**
     * An abnormal state: a slanted bar in its colour, the icon, its name and how far it has gone, over a backing that
     * fades out to the right. It wipes open on arrival and flashes.
     */
    private static void drawCondition(GuiGraphics g, HudFrame f, Conditions.Condition c, float y, float appear) {
        float px = f.px;
        float full = conditionWidth(c), w = full * Easing.outCubic(appear);
        boolean flash = appear < 1F && Easing.blink(f.now, 4F);
        HudDraw.fadeRight(g, 0, y, w, y + 10F, f.c(c.color(), 0.22F * appear));
        HudDraw.parallelogram(g, 0, y, 2F, 10F, 1.5F, f.c(c.color(), appear));
        c.icon().draw(g, 4.5F, y + 1F, 8F, f.c(flash ? NeonPalette.WHITE : c.color(), appear));
        if (appear > 0.5F) {
            float a = (appear - 0.5F) / 0.5F;
            HudText.draw(g, HudText.title(c.label()), 15F, y + (10F - HudText.Kind.TITLE.cap()) / 2F, f.c(NeonPalette.WHITE, a), px);
            if (!Float.isNaN(c.level())) {
                Component pct = HudText.data(Math.round(c.level() * 100) + "%");
                HudText.drawRight(g, pct, full - 4F, y + (10F - HudText.Kind.DATA.cap()) / 2F, f.c(c.color(), a), px);
            }
        }
    }

    /** An effect: its icon inside a thin ring that drains clockwise with the remaining time; its level in a corner. */
    private void drawEffect(GuiGraphics g, HudFrame f, MobEffectInstance e, float cx, float cy, float boot) {
        float px = f.px;
        Holder<MobEffect> effect = e.getEffect();
        int color = EffectIcons.color(effect);
        int max = maxDuration.merge(effect, e.getDuration(), Math::max);
        float left = e.isInfiniteDuration() ? 1F : Math.clamp(e.getDuration() / (float) Math.max(1, max), 0F, 1F);
        boolean ending = !e.isInfiniteDuration() && e.endsWithin(200);
        float alpha = boot * (ending && Easing.blink(f.now, 10F) ? 0.45F : 1F);

        HudDraw.ring(g, cx, cy, RING - px, px, 32, f.c(color, 0.22F * boot));
        HudDraw.arc(g, cx, cy, RING - px * 2, px * 2.5F, 0F, (float) (Math.PI * 2 * left), 32, f.c(color, 0.95F * boot));
        EffectIcons.icon(effect).draw(g, cx - 4.5F, cy - 4.5F, 9F, f.c(color, alpha));
        if (e.getAmplifier() > 0) {
            HudText.draw(g, HudText.label(roman(e.getAmplifier() + 1)), cx + RING - 2F, cy + RING - 4F, f.c(NeonPalette.WHITE, boot), px);
        }
    }

    private static String roman(int n) {
        return switch (n) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
