package com.waylays.chromeneon.client.hud.theme;

import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The HUD's two typefaces: {@link #HUD} (5x7, cut corners, for names and values) and {@link #MICRO} (3x5, for
 * technical labels). Both are uppercase by design. Long free text (notification bodies) keeps the game font.
 */
public final class HudText {
    public static final ResourceLocation HUD = ChromeNeon.id("hud");
    public static final ResourceLocation MICRO = ChromeNeon.id("micro");
    private static final Style HUD_STYLE = Style.EMPTY.withFont(HUD);
    private static final Style MICRO_STYLE = Style.EMPTY.withFont(MICRO);
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private HudText() {}

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static Component hud(String text) {
        return Component.literal(text.toUpperCase(Locale.ROOT)).withStyle(HUD_STYLE);
    }

    public static Component hud(Component text) {
        return hud(text.getString());
    }

    /** Micro labels have no accented glyphs: accents are dropped rather than falling back to another font. */
    public static Component micro(String text) {
        String plain = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        return Component.literal(plain.toUpperCase(Locale.ROOT)).withStyle(MICRO_STYLE);
    }

    public static Component micro(Component text) {
        return micro(text.getString());
    }

    public static int width(Component text) {
        return font().width(text);
    }

    /**
     * Draws at a fractional position (animations move text by sub-pixels), over a soft dark offset copy so light
     * text stays readable on snow or sky. Dark text (on a bright tab) gets no shadow.
     */
    public static void draw(GuiGraphics g, Component text, float x, float y, int argb) {
        float alpha = NeonPalette.alphaOf(argb);
        if (alpha < 0.02F) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        float lum = 0.3F * NeonPalette.red(argb) + 0.59F * NeonPalette.green(argb) + 0.11F * NeonPalette.blue(argb);
        if (lum > 0.35F) g.drawString(font(), text, 1, 1, NeonPalette.alpha(NeonPalette.VOID, 0.55F * alpha), false);
        g.drawString(font(), text, 0, 0, argb, false);
        g.pose().popPose();
    }

    /** Without the shadow: for text on solid panels. */
    public static void drawFlat(GuiGraphics g, Component text, float x, float y, int argb) {
        if (NeonPalette.alphaOf(argb) < 0.02F) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.drawString(font(), text, 0, 0, argb, false);
        g.pose().popPose();
    }

    public static void drawRight(GuiGraphics g, Component text, float right, float y, int argb) {
        draw(g, text, right - width(text), y, argb);
    }

    /** Neon text: a faint halo one pixel around, then the text itself. Reserved for the few values read first. */
    public static void drawGlow(GuiGraphics g, Component text, float x, float y, int argb, float glow) {
        int halo = NeonPalette.fade(argb, 0.22F * glow);
        if (glow > 0.01F) {
            drawFlat(g, text, x - 1, y, halo);
            drawFlat(g, text, x + 1, y, halo);
            drawFlat(g, text, x, y - 1, halo);
            drawFlat(g, text, x, y + 1, halo);
        }
        draw(g, text, x, y, argb);
    }

    /** First {@code fraction} of the text, for data that types itself in. */
    public static String reveal(String text, float fraction) {
        int n = Math.round(text.length() * Math.clamp(fraction, 0F, 1F));
        return text.substring(0, n);
    }
}
