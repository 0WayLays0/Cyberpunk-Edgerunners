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
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * The notification stack. No boxes: each message floats over a backing that fades out to the right, held by a
 * slanted accent bar in its colour. A hairline under the title carries the message's code at its end and drains
 * with its remaining time. Arrival: the bar rises, the line draws itself, the title types in, the body fades in.
 * Leaving: it slides back out while the messages below close the gap. Alerts shake and carry hazard stripes.
 */
public final class NotificationsGroup extends HudGroup {
    static final float W = 170F, GAP = 5F, HEADER = 11F, LINE = 7.5F;
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
        List<FormattedCharSequence> lines = HudText.font().split(HudText.body(n.message), (int) (W - 26F));
        return lines.subList(0, Math.min(f.minimal ? 1 : 2, lines.size()));
    }

    private static float cardHeight(Notification n, HudFrame f) {
        return HEADER + lines(n, f).size() * LINE + (n.progress != null ? 6F : 0F) + 3F;
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
        float px = f.px;
        NotificationStyle style = NotificationStyle.of(n.kind);
        float age = f.now - n.shownAt;
        float leave = n.leaving() ? Easing.inCubic((f.now - n.leaveAt) / NotificationQueue.LEAVE) : 0F;
        float dir = fromRight ? 1F : -1F;
        float x = dir * leave * (W + 12F);
        if (style.glitch() && age < 10F && f.glitch && Easing.blink(f.now, 2F)) x += dir * (age < 5F ? 3F : -2F);
        float alpha = Easing.clamp(age / 4F) * (1F - leave);
        if (alpha <= 0.01F) return;
        float h = cardHeight(n, f);
        int color = style.color();

        float bar = Easing.outCubic(age / 5F);
        float line = Easing.outCubic((age - 2F) / 8F);
        HudDraw.veil(g, x, y - 1F, x + W * (0.4F + 0.6F * line), y + h + 1F, 3F, f.c(NeonPalette.VOID, 0.65F * alpha), 0);
        HudDraw.parallelogram(g, x, y + h * (1F - bar), 2.2F, h * bar, 1.2F, f.c(color, alpha));
        if (style.hazard()) HudDraw.stripes(g, x + W - 30F, y, 26F, 2F, 3F, f.c(color, 0.85F * alpha));

        HudIcon icon = n.icon != null ? n.icon : style.icon();
        icon.draw(g, x + 6F, y + 1.5F, 7F, f.c(color, alpha));
        Component code = HudText.label(style.code());
        float codeW = f.minimal ? 0F : HudText.width(code) + 6F;
        String title = fit(HudText.title(n.title).getString(), W - 16F - 4F - codeW);
        float type = Easing.clamp((age - 3F) / 8F);
        HudText.draw(g, HudText.title(HudText.reveal(title, type)), x + 16F, y + 2F, f.c(NeonPalette.WHITE, alpha), px);

        // The rule under the title: draws itself, drains with the time left, the code at its end
        float ruleY = y + HEADER - 1.5F, ruleX0 = x + 6F, ruleX1 = x + 6F + (W - 10F) * line;
        HudDraw.rect(g, ruleX0, ruleY, ruleX1, ruleY + px, f.c(color, 0.3F * alpha));
        float left = n.progress != null || n.leaving() ? 1F : Easing.clamp((n.expiresAt - f.now) / Math.max(1F, n.expiresAt - n.shownAt));
        HudDraw.rect(g, ruleX0, ruleY, ruleX0 + (ruleX1 - ruleX0) * left, ruleY + px, f.c(color, 0.85F * alpha));
        HudDraw.rect(g, ruleX1 - px, ruleY - 2F, ruleX1, ruleY + px, f.c(color, 0.85F * alpha * line));
        if (!f.minimal) HudText.drawRight(g, code, ruleX1 - 2F, y + 2.5F, f.c(color, 0.75F * alpha * line), px);

        float ly = y + HEADER + 1F;
        float body = Easing.clamp((age - 6F) / 6F) * alpha;
        for (FormattedCharSequence seq : lines(n, f)) {
            g.pose().pushPose();
            g.pose().translate(x + 16F, ly, 0);
            g.drawString(HudText.font(), seq, 0, 0, f.c(NeonPalette.LAVENDER, body), false);
            g.pose().popPose();
            ly += LINE;
        }
        if (n.progress != null) {
            double p = Math.clamp(n.progress.getAsDouble(), 0.0, 1.0);
            int segs = 24;
            float sw = (W - 50F) / segs;
            for (int i = 0; i < segs; i++) {
                boolean on = (i + 0.5F) / segs <= p;
                HudDraw.parallelogram(g, x + 16F + i * sw, ly + 1F, sw - 0.7F, 2.5F, 0.8F, f.c(color, (on ? 0.95F : 0.18F) * alpha));
            }
            HudText.drawRight(g, HudText.data(Math.round(p * 100) + "%"), x + W - 6F, ly, f.c(color, alpha), px);
        }
    }

    /** The title cut to fit the room before the code label, with a final dot when cut. */
    private static String fit(String title, float room) {
        if (HudText.width(HudText.title(title)) <= room) return title;
        String t = title;
        while (t.length() > 1 && HudText.width(HudText.title(t + ".")) > room) t = t.substring(0, t.length() - 1);
        return t.trim() + ".";
    }

    /** How many messages fit between the group's top and the experience bar (its offset scales with the HUD). */
    public static int room(float guiHeight, float scale, float top) {
        return Math.max(1, (int) ((guiHeight - top * scale - 38F) / (34F * scale)));
    }
}
