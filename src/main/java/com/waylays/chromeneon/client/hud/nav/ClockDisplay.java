package com.waylays.chromeneon.client.hud.nav;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/**
 * The world clock: hours and minutes of the Minecraft day (06:00 at sunrise) in large HUD digits, the colon beating
 * each real second, a day/night mark, a line with the sun's position, the day number and phase, and the hour
 * sliding in when it changes. Dimensions with a frozen sky (the Nether, the End) have no time: the clock says so.
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

    public static float width(HudFrame f) {
        return f.minimal ? 34F : 64F;
    }

    public float height(HudFrame f) {
        return f.minimal ? 8F : 27F;
    }

    /**
     * Layout: the time in large digits with the day/night mark beside it, the sun's line under it, then the day
     * number and the phase of the day.
     */
    public void draw(GuiGraphics g, HudFrame f, ClientLevel level, float x, float y, float width, float boot) {
        boolean frozen = level.dimensionType().hasFixedTime();
        long dayTime = level.getDayTime();
        int[] hm = time(dayTime);
        if (hm[0] != lastHour) {
            if (lastHour >= 0) hourChangedAt = f.now;
            lastHour = hm[0];
        }
        float slide = Easing.outCubic(Easing.progress(f.now, hourChangedAt, 10F));
        boolean colon = (f.millis / 500) % 2 == 0;
        float scale = f.minimal ? 1F : 2F;
        int white = f.c(NeonPalette.WHITE, boot);
        boolean night = hm[0] >= 19 || hm[0] < 6;

        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1F);
        if (frozen) {
            HudText.draw(g, HudText.hud("--:--"), 0, 0, f.c(NeonPalette.LAVENDER, 0.7F * boot));
        } else {
            Component hours = HudText.hud(String.format("%02d", hm[0]));
            float hy = (1F - slide) * 4F;
            HudText.draw(g, hours, 0, hy, NeonPalette.fade(white, 0.35F + 0.65F * slide));
            float hw = HudText.width(hours);
            HudText.draw(g, HudText.hud(":"), hw, 0, NeonPalette.fade(white, colon ? 1F : 0.3F));
            HudText.draw(g, HudText.hud(String.format("%02d", hm[1])), hw + HudText.width(HudText.hud(":")), 0, white);
        }
        g.pose().popPose();
        if (f.minimal) return;

        float right = x + width;
        // Day/night mark in the corner left by the digits
        int mark = night ? NeonPalette.VIOLET_HOT : NeonPalette.CYAN;
        (night ? HudIcon.EYE : HudIcon.GLOW).draw(g, right - 9, y + 3, f.c(frozen ? NeonPalette.LAVENDER : mark, 0.85F * boot));

        float by = y + 17;
        HudDraw.hLine(g, x, right, by, f.accent(0.35F * boot));
        if (!frozen) {
            float sun = Math.floorMod(dayTime, 24000L) / 24000F;
            float nightStart = 13000F / 24000F, nightEnd = 23000F / 24000F;
            HudDraw.rect(g, x + width * nightStart, by, x + width * nightEnd, by + 1, f.c(NeonPalette.VIOLET_DEEP, boot));
            float sx = x + width * sun;
            HudDraw.rect(g, sx - 1, by - 1.5F, sx + 1, by + 2.5F, f.c(mark, boot));
        }
        Component day = HudText.micro(frozen ? "NO SYNC" : "DAY " + String.format("%03d", dayTime / 24000L + 1));
        HudText.draw(g, day, x, y + 21, f.c(NeonPalette.CYAN_SOFT, boot));
        Component phase = HudText.micro(frozen ? "--" : night ? "NIGHT" : "DAYLIGHT");
        HudText.drawRight(g, phase, right, y + 21, f.c(night ? NeonPalette.VIOLET_HOT : NeonPalette.LAVENDER, 0.8F * boot));
    }
}
