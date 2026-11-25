package com.waylays.chromeneon.client.hud.vitals;

import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudGroup;
import com.waylays.chromeneon.client.hud.HudVisibility;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Top-left cluster: a technical label, health, then armor and RAM indented under it. The indent and the label
 * make the block asymmetric on purpose. Health and armor appear only where the player can be hurt (as in vanilla);
 * RAM is always shown.
 */
public final class VitalsGroup extends HudGroup {
    private static final float LABEL_ROW = 8F, ROW_GAP = 4F, INDENT = 12F;

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

    @Override
    protected float width(HudFrame f) {
        float w = 40F;
        if (healthShown()) w = Math.max(w, HealthBar.width(ChromeHud.vitals(), f));
        if (armorShown(f)) w = Math.max(w, INDENT + ArmorBar.width(ChromeHud.vitals()));
        if (RamCells.active()) w = Math.max(w, INDENT + RamCells.width(f, ChromeHud.ram()));
        return w;
    }

    @Override
    protected float height(HudFrame f) {
        float h = f.minimal ? 0F : LABEL_ROW;
        if (healthShown()) h += HealthBar.TAB_H + ROW_GAP + 1;
        if (armorShown(f)) h += ArmorBar.H + ROW_GAP;
        if (RamCells.active()) h += RamCells.H + ROW_GAP;
        return h;
    }

    @Override
    protected void draw(GuiGraphics g, HudFrame f) {
        VitalsTracker t = ChromeHud.vitals();
        float boot = boot(f);
        float y = 0F;
        if (!f.minimal) {
            Component label = HudText.micro("BIO.MON //");
            HudText.draw(g, label, 0, 0, f.c(NeonPalette.CYAN_SOFT, 0.7F * boot));
            if (healthShown()) {
                Component state = HudText.micro(HealthBar.state(f, t));
                int color = t.critical || t.health <= 0F ? NeonPalette.RED
                        : f.now - t.damageAt < 40F ? NeonPalette.ORANGE : NeonPalette.CYAN;
                HudText.draw(g, state, HudText.width(label) + 3, 0, f.c(color, boot));
            }
            y = LABEL_ROW;
        }
        if (healthShown()) {
            HealthBar.draw(g, f, t, 0, y, boot);
            y += HealthBar.TAB_H + ROW_GAP + 1;
        }
        if (armorShown(f)) {
            ArmorBar.draw(g, f, t, INDENT, y, f.boot(4));
            y += ArmorBar.H + ROW_GAP;
        }
        if (RamCells.active()) {
            RamCells.draw(g, f, ChromeHud.ram(), INDENT, y, f.boot(8));
        }
    }
}
