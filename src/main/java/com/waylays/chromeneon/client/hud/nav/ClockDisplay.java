package com.waylays.chromeneon.client.hud.nav;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/**
 * The world clock, right-aligned under the map: the time of the Minecraft day (06:00 at sunrise) with the colon
 * beating each real second and the hour sliding in when it changes; on its left the day number and phase; under it
 * the day line with the sun's position and the night shaded. Dimensions with a frozen sky have no time: it says so.
 */
public final class ClockDisplay {
    private int lastHour = -1;
    private float hourChangedAt = -1000F;

    /** Hours (0-23) and minutes of a day time, 0 being 06:00. */
    public static int[] time(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        int hours = (int) ((t / 1000 + 6) % 24);
        int minutes = (int) ((t % 1000) * 60 / 1000);
        return new int[] {hours, minutes};
    }

    public float height(HudFrame f) {
        return f.minimal ? HudText.Kind.VALUE.cap() + 2F : HudText.Kind.VALUE.cap() + 8F;
    }

    public void draw(GuiGraphics g, HudFrame f, ClientLevel level, float x, float y, float width, float boot) {
        float px = f.px;
        boolean frozen = level.dimensionType().hasFixedTime();
        long dayTime = level.getDayTime();
        int[] hm = time(dayTime);
        if (hm[0] != lastHour) {
            if (lastHour >= 0) hourChangedAt = f.now;
            lastHour = hm[0];
        }
        float slide = Easing.outCubic(Easing.progress(f.now, hourChangedAt, 10F));
        boolean colon = (f.millis / 500) % 2 == 0;
        boolean night = hm[0] >= 19 || hm[0] < 6;
        int white = f.c(NeonPalette.WHITE, boot);
        float right = x + width;
        HudDraw.veil(g, x - 14F, y - 3F, right + 2F, y + height(f) + 6F, 5F, 0, f.c(NeonPalette.VOID, 0.45F * boot));

        if (frozen) {
            HudText.drawRight(g, HudText.value("--:--"), right, y, f.c(NeonPalette.LAVENDER, 0.7F * boot), px);
        } else {
            Component minutes = HudText.value(String.format("%02d", hm[1]));
            Component sep = HudText.value(":");
            Component hours = HudText.value(String.format("%02d", hm[0]));
            float mx = right - HudText.width(minutes), sx = mx - HudText.width(sep) - 0.5F, hx = sx - HudText.width(hours) - 0.5F;
            HudText.draw(g, minutes, mx, y, white, px);
            HudText.draw(g, sep, sx, y, NeonPalette.fade(white, colon ? 1F : 0.25F), px);
            HudText.draw(g, hours, hx, y + (1F - slide) * 3F, NeonPalette.fade(white, 0.35F + 0.65F * slide), px);
        }
        if (f.minimal) return;
        int mark = night ? NeonPalette.VIOLET_HOT : NeonPalette.CYAN;
        Component day = HudText.label(frozen ? "NO SYNC" : "DAY " + String.format("%03d", dayTime / 24000L + 1));
        HudText.draw(g, day, x, y, f.c(NeonPalette.CYAN_SOFT, boot), px);
        Component phase = HudText.label(frozen ? "--" : night ? "NIGHT" : "DAYLIGHT");
        HudText.draw(g, phase, x, y + HudText.Kind.LABEL.cap() + 2F, f.c(night ? NeonPalette.VIOLET_HOT : NeonPalette.LAVENDER, 0.8F * boot), px);

        float by = y + HudText.Kind.VALUE.cap() + 4F;
        HudDraw.rect(g, x, by, right, by + px, f.accent(0.4F * boot));
        if (!frozen) {
            float sun = Math.floorMod(dayTime, 24000L) / 24000F;
            HudDraw.rect(g, x + width * (13000F / 24000F), by - px, x + width * (23000F / 24000F), by + px * 2,
                    f.c(NeonPalette.VIOLET_DEEP, boot));
            float sxp = x + width * sun;
            HudDraw.diamond(g, sxp, by + px / 2, 1.6F, f.c(mark, boot));
        }
    }
}
