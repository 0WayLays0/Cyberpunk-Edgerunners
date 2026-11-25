package com.waylays.chromeneon.client.hud;

import com.waylays.chromeneon.client.cyberware.ClientCyberware;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.client.input.ChromeKeys;
import com.waylays.chromeneon.common.config.ClientConfig;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.CyberwareLoadout;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantState;
import com.waylays.chromeneon.common.cyberware.implants.ChronosAccelerator;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom right: one dial per implant that does something, in the order of the body. Each dial is the implant's icon
 * in a ring that tells its state: draining while it runs, filling while it cools down, a gauge for the plating,
 * pips for charges; its key under it. A malfunction turns the dial red; a switched-off implant is dimmed.
 */
public final class ImplantsGroup extends HudGroup {
    private static final float R = 7.5F, STRIDE = 21F, H = 27F;

    private record Dial(Implant implant, HudIcon icon, KeyMapping key) {}

    public ImplantsGroup() {
        super("implants", ClientConfig.IMPLANTS, 16);
    }

    private static List<Dial> dials() {
        List<Dial> d = new ArrayList<>();
        ClientCyberware.accelerator().ifPresent(f -> d.add(new Dial(f.implant(), HudIcon.CHRONO, ChromeKeys.ACCELERATOR)));
        add(d, ModImplants.REAPER_BLADES.get(), HudIcon.BLADE, ChromeKeys.BLADES);
        add(d, ModImplants.ARGUS_OPTICS.get(), HudIcon.EYE, ChromeKeys.ANALYSIS);
        add(d, ModImplants.SPRING_TENDONS.get(), HudIcon.SPRING, null);
        add(d, ModImplants.CARAPACE_DERMIS.get(), HudIcon.SHIELD, null);
        add(d, ModImplants.HEMOSTAT_PUMP.get(), HudIcon.HEART_PULSE, null);
        return d;
    }

    private static void add(List<Dial> list, Implant implant, HudIcon icon, KeyMapping key) {
        if (!ClientCyberware.of(implant).isEmpty()) list.add(new Dial(implant, icon, key));
    }

    @Override
    protected boolean active(HudFrame f) {
        return !dials().isEmpty();
    }

    @Override
    protected float width(HudFrame f) {
        return dials().size() * STRIDE;
    }

    @Override
    protected float height(HudFrame f) {
        return H;
    }

    @Override
    protected void draw(GuiGraphics g, HudFrame f) {
        float px = f.px, boot = boot(f);
        long now = ClientCyberware.now();
        List<Dial> dials = dials();
        float w = dials.size() * STRIDE;
        HudDraw.veil(g, -6F, -3F, w + 4F, H + 2F, 5F, 0, f.c(NeonPalette.VOID, 0.4F * boot));
        HudDraw.rect(g, 0, H - 1F, w * Easing.outCubic(boot), H - 1F + px, f.accent(0.45F));
        for (int i = 0; i < dials.size(); i++) {
            Dial dial = dials.get(i);
            CyberwareLoadout.Entry entry = ClientCyberware.first(dial.implant()).orElseThrow();
            dial(g, f, dial, entry.installed().state(), i * STRIDE + STRIDE / 2F, R + 1F, now, boot);
        }
    }

    private static void dial(GuiGraphics g, HudFrame f, Dial dial, ImplantState s, float cx, float cy, long now, float boot) {
        float px = f.px;
        boolean broken = s.malfunctioning(now);
        boolean on = s.enabled && !broken;
        int color = broken ? NeonPalette.RED : !s.enabled ? NeonPalette.LAVENDER : NeonPalette.CYAN;
        float alpha = (on ? 1F : 0.45F) * boot;
        HudDraw.ring(g, cx, cy, R - px, px, 36, f.c(color, 0.25F * boot));
        float fill = 0F;
        int fillColor = color;
        Implant implant = dial.implant();
        if (implant instanceof ChronosAccelerator) {
            // Both spans start at the last change (start, then end): the ring is right whatever the model and reductions
            if (s.active && now < s.activeUntil) {
                fill = (s.activeUntil - now) / (float) Math.max(1L, s.activeUntil - s.changedAt);
                fillColor = NeonPalette.VIOLET_HOT;
            } else if (now < s.cooldownUntil) {
                fill = 1F - (s.cooldownUntil - now) / (float) Math.max(1L, s.cooldownUntil - s.changedAt);
                fillColor = NeonPalette.LAVENDER;
            } else fill = 1F;
        } else if (implant == ModImplants.REAPER_BLADES.get()) {
            fill = now < s.cooldownUntil ? 1F - (s.cooldownUntil - now) / (float) ServerConfig.LUNGE_COOLDOWN.get() : 1F;
            fillColor = s.active ? NeonPalette.VIOLET_HOT : NeonPalette.LAVENDER;
        } else if (implant == ModImplants.ARGUS_OPTICS.get()) {
            if (now < s.activeUntil) {
                fill = (s.activeUntil - now) / (float) ServerConfig.TACTICAL_DURATION.get();
            } else if (now < s.cooldownUntil) {
                fill = 1F - (s.cooldownUntil - now) / (float) ServerConfig.TACTICAL_COOLDOWN.get();
                fillColor = NeonPalette.LAVENDER;
            } else fill = s.active ? 1F : 0F;
        } else if (implant == ModImplants.CARAPACE_DERMIS.get()) {
            fill = s.resource / ServerConfig.DERMIS_INTEGRITY.get().floatValue();
            fillColor = s.active ? NeonPalette.STEEL : NeonPalette.ORANGE;
        } else {
            fill = 1F;
        }
        if (broken) fill = Easing.blink(f.now, 8F) ? 1F : 0F;
        HudDraw.arc(g, cx, cy, R - px * 2.5F, px * 2.5F, 0F, (float) (Math.PI * 2 * Math.clamp(fill, 0F, 1F)), 36, f.c(fillColor, alpha));
        dial.icon().draw(g, cx - 4.5F, cy - 4.5F, 9F, f.c(broken ? NeonPalette.RED : s.active ? NeonPalette.WHITE : color, alpha));
        // Charges as pips over the ring
        int charges = implant == ModImplants.HEMOSTAT_PUMP.get() ? s.charges
                : implant == ModImplants.SPRING_TENDONS.get() ? s.charges : -1;
        int max = implant == ModImplants.HEMOSTAT_PUMP.get() ? ServerConfig.PUMP_CHARGES.get()
                : implant == ModImplants.SPRING_TENDONS.get() ? ServerConfig.EXTRA_JUMPS.get() + 1 : 0;
        for (int i = 0; i < max && charges >= 0; i++) {
            float a = (float) (-Math.PI / 2 + (i - (max - 1) / 2F) * 0.45F);
            float x = cx + (float) Math.cos(a) * (R + 2.2F), y = cy + (float) Math.sin(a) * (R + 2.2F);
            HudDraw.diamond(g, x, y, 1.2F, f.c(i < charges ? NeonPalette.CYAN : NeonPalette.LAVENDER, i < charges ? alpha : 0.3F * boot));
        }
        Component label = dial.key() != null ? HudText.label(dial.key().getTranslatedKeyMessage().getString())
                : HudText.label(implant == ModImplants.SPRING_TENDONS.get() ? "AUTO" : implant == ModImplants.HEMOSTAT_PUMP.get() ? "AUTO" : "PASSIVE");
        HudText.draw(g, label, cx - HudText.width(label) / 2F, cy + R + 3F, f.c(on ? NeonPalette.LAVENDER : NeonPalette.RED, 0.9F * boot), px);
    }
}
