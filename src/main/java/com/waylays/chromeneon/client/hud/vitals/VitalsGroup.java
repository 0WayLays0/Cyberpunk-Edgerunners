package com.waylays.chromeneon.client.hud.vitals;

import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudGroup;
import com.waylays.chromeneon.client.hud.HudVisibility;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Top-left: health on top, then a rail drops from under the health number and the other readings hang off it
 * (state line, armor, RAM), each joined by a short branch with a node. Health and armor appear only where the
 * player can be hurt (as in vanilla); RAM is always shown.
 */
public final class VitalsGroup extends HudGroup {
    private static final float RAIL_X = 2F, BRANCH = 7F, ROW = 8.5F;

    public VitalsGroup() {
        super("vitals", ClientConfig.VITALS, 0);
    }

    @Override
    protected boolean active(HudFrame f) {
        return HudVisibility.vulnerable() || RamCells.active();
    }

    private static boolean healthShown() {
        return HudVisibility.vulnerable();
    }

    private static boolean armorShown(HudFrame f) {
        return healthShown() && (!f.minimal || ChromeHud.vitals().armor > 0);
    }

    private static float healthHeight() {
        return HealthBar.labelY(0F);
    }

    @Override
    protected float width(HudFrame f) {
        float w = 40F;
        if (healthShown()) w = Math.max(w, HealthBar.width(ChromeHud.vitals()));
        if (armorShown(f)) w = Math.max(w, BRANCH + ArmorBar.width(ChromeHud.vitals()));
        if (RamCells.active()) w = Math.max(w, BRANCH + RamCells.width(f, ChromeHud.ram()));
        return w;
    }

    @Override
    protected float height(HudFrame f) {
        float h = healthShown() ? healthHeight() + (f.minimal ? 0F : ROW) : 0F;
        if (armorShown(f)) h += ROW;
        if (RamCells.active()) h += ROW;
        return h + 2F;
    }

    @Override
    protected void draw(GuiGraphics g, HudFrame f) {
        float px = f.px;
        VitalsTracker t = ChromeHud.vitals();
        float boot = boot(f);
        float y = 0F;
        HudDraw.veil(g, -8F, -4F, width(f) + 10F, height(f) + 3F, 6F, f.c(NeonPalette.VOID, 0.42F * boot), 0);
        if (healthShown()) {
            HealthBar.draw(g, f, t, 0, 0, boot);
            y = healthHeight();
            if (!f.minimal) {
                Component label = HudText.label("BIO-MONITOR //");
                HudText.draw(g, label, BRANCH, y, f.c(NeonPalette.CYAN_SOFT, 0.75F * boot), px);
                int color = t.critical || t.health <= 0F ? NeonPalette.RED
                        : f.now - t.damageAt < 40F ? NeonPalette.ORANGE : NeonPalette.CYAN;
                HudText.draw(g, HudText.label(HealthBar.state(f, t)), BRANCH + HudText.width(label) + 2, y, f.c(color, boot), px);
                y += ROW;
            }
        }
        float railTop = healthShown() ? HealthBar.labelY(0F) - 2F : 0F;
        float rows = 0F;
        if (armorShown(f)) {
            branch(g, f, y, f.boot(4));
            ArmorBar.draw(g, f, t, BRANCH + 3F, y, f.boot(4));
            y += ROW;
            rows++;
        }
        if (RamCells.active()) {
            branch(g, f, y, f.boot(8));
            RamCells.draw(g, f, ChromeHud.ram(), BRANCH + 3F, y, f.boot(8));
            y += ROW;
            rows++;
        }
        if (rows > 0F) {
            float bottom = y - ROW + 2.5F;
            float reach = railTop + (bottom - railTop) * Easing.outCubic(boot);
            HudDraw.rect(g, RAIL_X, railTop, RAIL_X + px, reach, f.accent(0.6F));
        }
    }

    /** A branch from the rail to a row, with a node where it leaves the rail. */
    private static void branch(GuiGraphics g, HudFrame f, float y, float boot) {
        float cy = y + 2.5F;
        HudDraw.rect(g, RAIL_X, cy, RAIL_X + (BRANCH) * Easing.outCubic(boot), cy + f.px, f.accent(0.6F * boot));
        HudDraw.diamond(g, RAIL_X + f.px / 2, cy + f.px / 2, 1.3F, f.accent(boot));
    }
}
