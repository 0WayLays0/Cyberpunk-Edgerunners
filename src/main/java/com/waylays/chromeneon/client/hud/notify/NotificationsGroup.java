package com.waylays.chromeneon.client.hud.notify;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudGroup;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * The notification stack. A card slides in from its side while a bright edge sweeps across it and its title types
 * in; a line drains as its time runs out; on leaving it slides back and the cards below close the gap smoothly.
 * Alerts and errors arrive shaking, with hazard stripes for alerts.
 */
public final class NotificationsGroup extends HudGroup {
    static final float W = 176F, GAP = 4F, HEADER = 13F, LINE = 9F;
    private final NotificationQueue queue;

    public NotificationsGroup(NotificationQueue queue) {
        super("notifications", ClientConfig.NOTIFICATIONS, 12);
        this.queue = queue;
    }

    @Override
    protected boolean active(HudFrame f) {
        return !queue.visible().isEmpty();
    }

    @Override
    protected float width(HudFrame f) {
        return W;
    }

    @Override
    protected float height(HudFrame f) {
        float h = 0F;
        for (Notification n : queue.visible()) h += slot(f, n);
        return Math.max(1F, h);
    }

    private static List<FormattedCharSequence> lines(Notification n, HudFrame f) {
        if (n.message.getString().isEmpty()) return List.of();
        List<FormattedCharSequence> lines = HudText.font().split(n.message, (int) W - 22);
        return lines.subList(0, Math.min(f.minimal ? 1 : 2, lines.size()));
    }

    private static float cardHeight(Notification n, HudFrame f) {
        return HEADER + lines(n, f).size() * LINE + (n.progress != null ? 7F : 0F) + 5F;
    }

    private static float slot(HudFrame f, Notification n) {
        float enter = Easing.clamp((f.now - n.shownAt) / NotificationQueue.ENTER * 1.6F);
        float leave = n.leaving() ? Easing.clamp((f.now - n.leaveAt) / NotificationQueue.LEAVE) : 0F;
        return (cardHeight(n, f) + GAP) * Easing.outCubic(enter) * (1F - Easing.inOutCubic(leave));
    }

    @Override
    protected void draw(GuiGraphics g, HudFrame f) {
        boolean right = anchor().isRight();
        float y = 0F;
        for (Notification n : queue.visible()) {
            card(g, f, n, y, right);
            y += slot(f, n);
        }
    }

    private static void card(GuiGraphics g, HudFrame f, Notification n, float y, boolean fromRight) {
        NotificationStyle style = NotificationStyle.of(n.kind);
        float age = f.now - n.shownAt;
        float enter = Easing.outCubic(age / NotificationQueue.ENTER);
        float leave = n.leaving() ? Easing.inCubic((f.now - n.leaveAt) / NotificationQueue.LEAVE) : 0F;
        float dir = fromRight ? 1F : -1F;
        float x = dir * ((1F - enter) * (W + 12F) + leave * (W + 12F));
        if (style.glitch() && age < 10F && f.glitch && Easing.blink(f.now, 2F)) x += dir * (age < 5F ? 3F : -2F);
        float alpha = enter * (1F - leave);
        if (alpha <= 0.01F) return;
        float h = cardHeight(n, f);
        int color = style.color();

        HudDraw.cutPanel(g, x, y, W, h, 6, 0, 4, 0, f.c(NeonPalette.VOID, 0.74F * alpha));
        HudDraw.cutFrame(g, x, y, W, h, 6, 0, 4, 0, f.c(color, 0.35F * alpha));
        HudDraw.rect(g, x, y + 6, x + 2, y + h, f.c(color, alpha));
        if (style.hazard()) HudDraw.stripes(g, x + W - 34, y + 1, 30, 3, 4, f.c(color, 0.8F * alpha));

        HudIcon icon = n.icon != null ? n.icon : style.icon();
        icon.draw(g, x + 6, y + 3, f.c(color, alpha));
        var code = HudText.micro(style.code());
        float codeW = f.minimal ? 0F : HudText.width(code) + 6F;
        String title = fit(HudText.hud(n.title).getString(), W - 18F - 5F - codeW);
        float type = Easing.clamp((age - 3F) / 8F);
        HudText.draw(g, HudText.hud(HudText.reveal(title, type)), x + 18, y + 4, f.c(NeonPalette.WHITE, alpha));
        if (!f.minimal) HudText.drawRight(g, code, x + W - 5, y + (style.hazard() ? 6 : 4), f.c(color, 0.7F * alpha));

        float ly = y + HEADER;
        float body = Easing.clamp((age - 6F) / 6F) * alpha;
        for (FormattedCharSequence line : lines(n, f)) {
            g.drawString(HudText.font(), line, Math.round(x + 18), Math.round(ly), f.c(NeonPalette.LAVENDER, body), false);
            ly += LINE;
        }
        if (n.progress != null) {
            double p = Math.clamp(n.progress.getAsDouble(), 0.0, 1.0);
            int segs = 16;
            float sw = (W - 52F) / segs;
            for (int i = 0; i < segs; i++) {
                boolean on = (i + 0.5F) / segs <= p;
                HudDraw.parallelogram(g, x + 18 + i * sw, ly + 1, sw - 1.2F, 3, 1, f.c(color, (on ? 0.95F : 0.18F) * alpha));
            }
            HudText.drawRight(g, HudText.micro(Math.round(p * 100) + "%"), x + W - 5, ly, f.c(color, alpha));
        } else if (!n.leaving()) {
            float left = Easing.clamp((n.expiresAt - f.now) / Math.max(1F, n.expiresAt - n.shownAt));
            HudDraw.rect(g, x + 2, y + h - 1, x + 2 + (W - 6) * left, y + h, f.c(color, 0.6F * alpha));
        }
        // The arrival sweep: a bright vertical edge crossing the card
        float sweep = age / 10F;
        if (sweep < 1F) {
            float sx = x + W * Easing.outCubic(sweep);
            HudDraw.rect(g, sx, y + 1, sx + 1.5F, y + h - 1, f.c(NeonPalette.WHITE, 0.8F * (1F - sweep)));
        }
    }

    /** The title cut to fit the room before the code label, with a final dot when cut. */
    private static String fit(String title, float room) {
        if (HudText.width(HudText.hud(title)) <= room) return title;
        String t = title;
        while (t.length() > 1 && HudText.width(HudText.hud(t + ".")) > room) t = t.substring(0, t.length() - 1);
        return t.trim() + ".";
    }

    /** How many cards fit between the group's top and the experience bar (its offset scales with the HUD). */
    public static int room(float guiHeight, float scale, float top) {
        return Math.max(1, (int) ((guiHeight - top * scale - 38F) / (48F * scale)));
    }
}
