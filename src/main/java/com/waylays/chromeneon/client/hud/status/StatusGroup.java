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
 * Status indicators, replacing the vanilla effect list. First row: abnormal states, as alert chips with their
 * label (they matter more). Second row: effects as icon chips with a draining duration line and their level;
 * harmful ones first. Minimal mode keeps the states and the harmful effects only.
 */
public final class StatusGroup extends HudGroup {
    private static final float CHIP = 13F, GAP = 3F, ROW = 18F;
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
        for (Conditions.Condition c : Conditions.of(player())) w += chipWidth(c) + GAP;
        return Math.max(w, Math.min(effects(f).size(), MAX_CHIPS) * (CHIP + GAP) + 20F);
    }

    @Override
    protected float height(HudFrame f) {
        boolean states = !Conditions.of(player()).isEmpty(), effects = !effects(f).isEmpty();
        return (states ? ROW : 0F) + (effects ? ROW : 0F);
    }

    private static float chipWidth(Conditions.Condition c) {
        return CHIP + HudText.width(HudText.hud(c.label())) + 8F + (Float.isNaN(c.level()) ? 0F : 18F);
    }

    @Override
    protected void draw(GuiGraphics g, HudFrame f) {
        float boot = boot(f);
        List<Conditions.Condition> states = Conditions.of(player());
        onset.keySet().retainAll(states.stream().map(Conditions.Condition::id).toList());
        float y = 0F, x = 0F;
        for (Conditions.Condition c : states) {
            float start = onset.computeIfAbsent(c.id(), k -> f.now);
            x += drawCondition(g, f, c, x, y, Easing.progress(f.now, start, 8F) * boot) + GAP;
        }
        if (!states.isEmpty()) y += ROW;

        List<MobEffectInstance> effects = effects(f);
        maxDuration.keySet().retainAll(effects.stream().map(MobEffectInstance::getEffect).toList());
        x = 0F;
        for (int i = 0; i < effects.size() && i < MAX_CHIPS; i++) {
            drawEffect(g, f, effects.get(i), x, y, boot);
            x += CHIP + GAP;
        }
        if (effects.size() > MAX_CHIPS) {
            HudText.draw(g, HudText.micro("+" + (effects.size() - MAX_CHIPS)), x + 1, y + 4, f.c(NeonPalette.LAVENDER));
        }
    }

    /** An alert chip: framed icon then the label on a tinted band; slides open on arrival. */
    private static float drawCondition(GuiGraphics g, HudFrame f, Conditions.Condition c, float x, float y, float appear) {
        float full = chipWidth(c), w = CHIP + (full - CHIP) * Easing.outCubic(appear);
        boolean flash = appear < 1F && Easing.blink(f.now, 4F);
        HudDraw.cutPanel(g, x, y, w, CHIP, 3, 0, 3, 0, f.c(NeonPalette.VOID, 0.72F));
        HudDraw.cutFrame(g, x, y, w, CHIP, 3, 0, 3, 0, f.c(c.color(), flash ? 1F : 0.85F));
        HudDraw.rect(g, x + CHIP, y + 2, x + w - 2, y + CHIP - 2, f.c(c.color(), 0.14F));
        c.icon().draw(g, x + 2, y + 2, f.c(flash ? NeonPalette.WHITE : c.color()));
        if (appear > 0.6F) {
            HudText.draw(g, HudText.hud(c.label()), x + CHIP + 3, y + 3, f.c(NeonPalette.WHITE, (appear - 0.6F) / 0.4F));
            if (!Float.isNaN(c.level())) {
                Component pct = HudText.micro(Math.round(c.level() * 100) + "%");
                HudText.drawRight(g, pct, x + w - 4, y + 4, f.c(c.color()));
            }
        }
        return full;
    }

    private void drawEffect(GuiGraphics g, HudFrame f, MobEffectInstance e, float x, float y, float boot) {
        Holder<MobEffect> effect = e.getEffect();
        int color = EffectIcons.color(effect);
        int max = maxDuration.merge(effect, e.getDuration(), Math::max);
        float left = e.isInfiniteDuration() ? 1F : Math.clamp(e.getDuration() / (float) Math.max(1, max), 0F, 1F);
        boolean ending = !e.isInfiniteDuration() && e.endsWithin(200);
        float alpha = boot * (ending && Easing.blink(f.now, 10F) ? 0.45F : 1F);

        HudDraw.cutPanel(g, x, y, CHIP, CHIP, 3, 0, 3, 0, f.c(NeonPalette.VOID, 0.65F * boot));
        HudDraw.cutFrame(g, x, y, CHIP, CHIP, 3, 0, 3, 0, f.c(color, 0.45F * boot));
        EffectIcons.icon(effect).draw(g, x + 2, y + 2, f.c(color, alpha));
        HudDraw.rect(g, x, y + CHIP + 1, x + CHIP, y + CHIP + 2, f.c(color, 0.18F * boot));
        HudDraw.rect(g, x, y + CHIP + 1, x + CHIP * left, y + CHIP + 2, f.c(color, 0.9F * boot));
        if (e.getAmplifier() > 0) {
            Component level = HudText.micro(roman(e.getAmplifier() + 1));
            float lw = HudText.width(level);
            HudDraw.rect(g, x + CHIP - lw - 1, y + CHIP - 6, x + CHIP + 1, y + CHIP, f.c(NeonPalette.VOID, 0.85F * boot));
            HudText.draw(g, level, x + CHIP - lw, y + CHIP - 5, f.c(NeonPalette.WHITE, boot));
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
