package com.waylays.chromeneon.client.hud.theme;

import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * The HUD's typography: vector fonts (Rajdhani for names and values, Share Tech Mono for data and labels) in six
 * styles. Each style exists once per GUI scale, rasterised at that scale, so text is drawn one texel per screen
 * pixel: sharp at any size, never pixelated. The top of the capitals sits at the drawing position.
 */
public final class HudText {
    public enum Kind {
        /** Tiny technical labels (monospace). */
        LABEL("label", 3.5F),
        /** Data and numbers in lists (monospace). */
        DATA("data", 4.2F),
        /** Running text (notification bodies). */
        BODY("body", 5.14F),
        /** Names and titles. */
        TITLE("title", 6.43F),
        /** Key values (health, clock). */
        VALUE("value", 9F),
        /** Large display numbers. */
        DISPLAY("display", 12.86F);

        private final String id;
        private final float cap;

        Kind(String id, float cap) {
            this.id = id;
            this.cap = cap;
        }

        /** Height of the capitals, in GUI pixels. */
        public float cap() {
            return cap;
        }
    }

    private static int variant = 2;

    private HudText() {}

    /** Picks the rasterisation matching the current scale (GUI scale times HUD scale). */
    public static void setScale(double effectiveScale) {
        variant = Math.clamp(Math.round((float) effectiveScale), 1, 6);
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static Component of(Kind kind, String text) {
        ResourceLocation id = ChromeNeon.id(kind.id + "_" + variant);
        return Component.literal(text).withStyle(Style.EMPTY.withFont(id));
    }

    public static Component of(Kind kind, Component text) {
        return of(kind, text.getString());
    }

    /** Uppercase label: tiny monospace, for technical tags. */
    public static Component label(String text) {
        return of(Kind.LABEL, text.toUpperCase(Locale.ROOT));
    }

    public static Component label(Component text) {
        return label(text.getString());
    }

    public static Component data(String text) {
        return of(Kind.DATA, text.toUpperCase(Locale.ROOT));
    }

    public static Component data(Component text) {
        return data(text.getString());
    }

    public static Component title(String text) {
        return of(Kind.TITLE, text.toUpperCase(Locale.ROOT));
    }

    public static Component title(Component text) {
        return title(text.getString());
    }

    public static Component body(Component text) {
        return of(Kind.BODY, text);
    }

    public static Component value(String text) {
        return of(Kind.VALUE, text);
    }

    public static Component display(String text) {
        return of(Kind.DISPLAY, text);
    }

    public static float width(Component text) {
        return font().width(text);
    }

    /** {@code text} in {@code kind}, cut to fit {@code room} with a final dot when cut. */
    public static Component fit(Kind kind, String text, float room) {
        if (width(of(kind, text)) <= room) return of(kind, text);
        String t = text;
        while (t.length() > 1 && width(of(kind, t + ".")) > room) t = t.substring(0, t.length() - 1);
        return of(kind, t.trim() + ".");
    }

    /**
     * Draws at a fractional position over a one-screen-pixel dark offset copy, so light text stays readable on snow
     * or sky without a heavy shadow. Dark text gets no shadow.
     */
    public static void draw(GuiGraphics g, Component text, float x, float y, int argb, float px) {
        float alpha = NeonPalette.alphaOf(argb);
        if (alpha < 0.02F) return;
        float lum = 0.3F * NeonPalette.red(argb) + 0.59F * NeonPalette.green(argb) + 0.11F * NeonPalette.blue(argb);
        if (lum > 0.35F) drawFlat(g, text, x + px, y + px, NeonPalette.alpha(NeonPalette.VOID, 0.6F * alpha));
        drawFlat(g, text, x, y, argb);
    }

    public static void drawRight(GuiGraphics g, Component text, float right, float y, int argb, float px) {
        draw(g, text, right - width(text), y, argb, px);
    }

    public static void drawFlat(GuiGraphics g, Component text, float x, float y, int argb) {
        if (NeonPalette.alphaOf(argb) < 0.02F) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.drawString(font(), text, 0, 0, argb, false);
        g.pose().popPose();
    }

    /** Neon text: faint copies around it, then the text. Reserved for the few values read first. */
    public static void drawGlow(GuiGraphics g, Component text, float x, float y, int argb, float px) {
        int halo = NeonPalette.fade(argb, 0.18F);
        float d = 2F * px;
        drawFlat(g, text, x - d, y, halo);
        drawFlat(g, text, x + d, y, halo);
        drawFlat(g, text, x, y - d, halo);
        drawFlat(g, text, x, y + d, halo);
        draw(g, text, x, y, argb, px);
    }

    /** First {@code fraction} of the text, for data that types itself in. */
    public static String reveal(String text, float fraction) {
        int n = Math.round(text.length() * Math.clamp(fraction, 0F, 1F));
        return text.substring(0, n);
    }
}
