package com.waylays.chromeneon.client.hud.theme;

import com.mojang.blaze3d.systems.RenderSystem;
import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The HUD's 9x9 icons ({@code textures/gui/hud_icons.png}, drawn by {@code tools/hud_assets.py}). They are white
 * and tinted when drawn, so one icon serves every state. The order matches the atlas.
 */
public enum HudIcon {
    HEART_PULSE, SHIELD, CHIP, SPEED, SLOW, STRENGTH, WEAKNESS, REGEN, POISON, WITHER, FIRE, FIRE_SHIELD, EYE, EYE_OFF,
    GHOST, WATER, JUMP, FEATHER, LEVITATE, HUNGER, NAUSEA, GLOW, LUCK, HASTE, FATIGUE, ABSORB, FREEZE, AIR, GOOD, BAD,
    INFO, CHECK, CROSS, POWER, SCAN, CALL, AVATAR, MARKER, HEART, FLAG, UPLOAD, HAZARD;

    public static final int SIZE = 9;
    private static final ResourceLocation ATLAS = ChromeNeon.id("textures/gui/hud_icons.png");
    private static final int ATLAS_W = 144, ATLAS_H = 36, COLUMNS = 16;

    public void draw(GuiGraphics g, float x, float y, int argb) {
        if (NeonPalette.alphaOf(argb) <= 0.01F) return;
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(NeonPalette.red(argb), NeonPalette.green(argb), NeonPalette.blue(argb), NeonPalette.alphaOf(argb));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        int u = (ordinal() % COLUMNS) * SIZE, v = (ordinal() / COLUMNS) * SIZE;
        g.blit(ATLAS, 0, 0, u, v, SIZE, SIZE, ATLAS_W, ATLAS_H);
        g.pose().popPose();
        g.setColor(1F, 1F, 1F, 1F);
    }
}
