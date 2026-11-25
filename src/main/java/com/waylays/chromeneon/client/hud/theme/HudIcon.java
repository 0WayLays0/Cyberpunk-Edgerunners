package com.waylays.chromeneon.client.hud.theme;

import com.mojang.blaze3d.systems.RenderSystem;
import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The HUD's line icons ({@code textures/gui/hud_icons.png}, drawn as vectors by {@code tools/hud_assets.py} at
 * 64 px with antialiasing). They are white, tinted when drawn, and sampled with linear filtering so they stay smooth
 * at any size. The order matches the atlas.
 */
public enum HudIcon {
    HEART_PULSE, SHIELD, CHIP, SPEED, SLOW, STRENGTH, WEAKNESS, REGEN, POISON, WITHER, FIRE, FIRE_SHIELD, EYE, EYE_OFF,
    GHOST, WATER, JUMP, FEATHER, LEVITATE, HUNGER, NAUSEA, GLOW, LUCK, HASTE, FATIGUE, ABSORB, FREEZE, AIR, GOOD, BAD,
    INFO, CHECK, CROSS, POWER, SCAN, CALL, AVATAR, MARKER, HEART, FLAG, UPLOAD, HAZARD, CHRONO, BLADE, SPRING;

    private static final ResourceLocation ATLAS = ChromeNeon.id("textures/gui/hud_icons.png");
    private static final int CELL = 64, ATLAS_W = 512, ATLAS_H = 384, COLUMNS = 8;
    private static AbstractTexture filtered;

    /** Draws the icon {@code size} GUI pixels wide at ({@code x}, {@code y}). */
    public void draw(GuiGraphics g, float x, float y, float size, int argb) {
        if (NeonPalette.alphaOf(argb) <= 0.01F) return;
        g.flush();
        smooth();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(NeonPalette.red(argb), NeonPalette.green(argb), NeonPalette.blue(argb), NeonPalette.alphaOf(argb));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size / CELL, size / CELL, 1F);
        int u = (ordinal() % COLUMNS) * CELL, v = (ordinal() / COLUMNS) * CELL;
        g.blit(ATLAS, 0, 0, u, v, CELL, CELL, ATLAS_W, ATLAS_H);
        g.pose().popPose();
        g.setColor(1F, 1F, 1F, 1F);
    }

    /** Linear filtering on the atlas (reapplied if a resource reload replaced the texture). */
    private static void smooth() {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(ATLAS);
        if (texture != filtered) {
            texture.setFilter(true, false);
            filtered = texture;
        }
    }
}
