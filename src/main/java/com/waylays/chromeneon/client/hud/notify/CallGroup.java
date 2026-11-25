package com.waylays.chromeneon.client.hud.notify;

import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudGroup;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.client.input.ChromeKeys;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * The incoming call: the caller's face (or a pattern generated from their name) in a bevelled frame with rings
 * turning around it while it rings, the name, the encrypted channel, a live waveform and the two answer keys as
 * they are bound. No box: it floats over a backing fading in from the screen edge. It slides in with a short glitch
 * and leaves flashing the answer's colour.
 */
public final class CallGroup extends HudGroup {
    private static final float W = 150F, H = 46F, AVATAR = 22F;

    public CallGroup() {
        super("call", ClientConfig.CALL, 0);
    }

    @Override
    protected boolean active(HudFrame f) {
        return ChromeHud.calls().call() != null;
    }

    @Override
    protected float width(HudFrame f) {
        return W;
    }

    @Override
    protected float height(HudFrame f) {
        return H;
    }

    @Override
    protected void draw(GuiGraphics g, HudFrame f) {
        float px = f.px;
        CallCenter calls = ChromeHud.calls();
        IncomingCall call = calls.call();
        float age = f.now - calls.startedAt();
        float enter = Easing.outCubic(age / 9F);
        boolean ringing = calls.outcome() == CallCenter.Outcome.RINGING;
        float out = ringing ? 0F : Easing.inCubic((f.now - calls.endedAt()) / CallCenter.OUTRO);
        float dir = anchor().isRight() ? 1F : -1F;
        float x = dir * ((1F - enter) * (W + 16F) + out * (W + 16F));
        if (age < 6F && f.glitch && Easing.blink(f.now, 2F)) x += dir * 3F;
        float alpha = enter * (1F - out);
        int ring = ringing ? NeonPalette.lerp(NeonPalette.VIOLET_HOT, NeonPalette.CYAN, Easing.pulse(f.now, 20F))
                : calls.outcome() == CallCenter.Outcome.ACCEPTED ? NeonPalette.CYAN : NeonPalette.RED;

        int back = f.c(NeonPalette.VOID, 0.62F * alpha);
        if (anchor().isRight()) HudDraw.veil(g, x - 6F, -3F, x + W + 8F, H + 3F, 5F, 0, back);
        else HudDraw.veil(g, x - 8F, -3F, x + W + 6F, H + 3F, 5F, back, 0);

        // Avatar in a bevelled frame, rings turning around it
        float ax = x + 8F, ay = 12F, acx = ax + AVATAR / 2F, acy = ay + AVATAR / 2F;
        HudDraw.outline(g, HudDraw.cutBox(ax - 1.5F, ay - 1.5F, AVATAR + 3F, AVATAR + 3F, 4F, 0F, 4F, 0F), px * 1.5F, f.c(ring, alpha));
        avatar(g, f, call, ax, ay, alpha);
        float spin = ringing ? f.now * 0.08F : 0F;
        HudDraw.dashedRing(g, acx, acy, AVATAR * 0.78F, px * 1.5F, 3, 0.22F, spin, f.c(ring, 0.8F * alpha));
        HudDraw.dashedRing(g, acx, acy, AVATAR * 0.9F, px, 8, 0.3F, -spin * 0.6F, f.c(ring, 0.35F * alpha));

        HudIcon.CALL.draw(g, x + 8F, 1.5F, 6F, f.c(NeonPalette.CYAN, (Easing.blink(f.now, 10F) || !ringing ? 1F : 0.35F) * alpha));
        Component header = HudText.label(Component.translatable(ringing ? "chromeneon.hud.call.incoming"
                : calls.outcome() == CallCenter.Outcome.ACCEPTED ? "chromeneon.hud.call.connected" : "chromeneon.hud.call.ended"));
        HudText.draw(g, header, x + 17F, 2.5F, f.c(NeonPalette.CYAN, alpha), px);
        int seconds = (int) (age / 20F);
        HudText.drawRight(g, HudText.data(String.format("%02d:%02d", seconds / 60, seconds % 60)), x + W - 4F, 2F, f.c(NeonPalette.LAVENDER, alpha), px);
        HudDraw.rect(g, x + 17F, 8.5F, x + W - 4F, 8.5F + px, f.c(ring, 0.5F * alpha));

        float tx = x + AVATAR + 18F;
        HudText.draw(g, HudText.title(truncate(call.caller().getString(), 20)), tx, 12F, f.c(NeonPalette.WHITE, alpha), px);
        HudText.draw(g, HudText.label(call.channel()), tx, 21F, f.c(NeonPalette.LAVENDER, 0.8F * alpha), px);
        for (int i = 0; i < 26; i++) {
            float level = ringing ? 0.2F + 0.8F * Math.abs((float) Math.sin(f.now * 0.35F + i * 0.9F) * (float) Math.cos(f.now * 0.13F + i * 0.4F)) : 0.1F;
            float bh = 0.5F + 4.5F * level;
            HudDraw.rect(g, tx + i * 2.2F, 30.5F - bh, tx + i * 2.2F + px * 3, 30.5F, f.c(NeonPalette.CYAN, 0.8F * alpha));
        }
        float ky = 36F;
        float aw = key(g, f, tx, ky, ChromeKeys.ACCEPT_CALL.getTranslatedKeyMessage(), Component.translatable("chromeneon.hud.call.accept"),
                NeonPalette.CYAN, alpha * (ringing || calls.outcome() == CallCenter.Outcome.ACCEPTED ? 1F : 0.3F));
        key(g, f, tx + aw + 8F, ky, ChromeKeys.DECLINE_CALL.getTranslatedKeyMessage(), Component.translatable("chromeneon.hud.call.decline"),
                NeonPalette.RED, alpha * (ringing || calls.outcome() != CallCenter.Outcome.ACCEPTED ? 1F : 0.3F));
    }

    /** A key tag and its action; returns the width used. */
    private static float key(GuiGraphics g, HudFrame f, float x, float y, Component key, Component action, int color, float alpha) {
        Component k = HudText.label(key);
        Component a = HudText.label(action);
        float kw = HudText.width(k) + 6F;
        HudDraw.parallelogram(g, x, y, kw, 6F, 1.5F, f.c(color, alpha));
        HudText.drawFlat(g, k, x + 3.5F, y + (6F - HudText.Kind.LABEL.cap()) / 2F, f.c(NeonPalette.VOID, alpha));
        HudText.draw(g, a, x + kw + 3F, y + (6F - HudText.Kind.LABEL.cap()) / 2F, f.c(color, alpha), f.px);
        return kw + 3F + HudText.width(a);
    }

    /** The caller's face when they are a player on this server; otherwise a symmetric pattern from their name. */
    private static void avatar(GuiGraphics g, HudFrame f, IncomingCall call, float x, float y, float alpha) {
        var connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = call.playerName() == null || connection == null ? null : connection.getPlayerInfo(call.playerName());
        if (info != null) {
            g.flush();
            g.setColor(1F, 1F, 1F, alpha * f.opacity);
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(AVATAR / 8F, AVATAR / 8F, 1F);
            PlayerFaceRenderer.draw(g, info.getSkin(), 0, 0, 8);
            g.pose().popPose();
            g.setColor(1F, 1F, 1F, 1F);
            return;
        }
        HudDraw.rect(g, x, y, x + AVATAR, y + AVATAR, f.c(NeonPalette.PANEL, 0.9F * alpha));
        int hash = call.caller().getString().hashCode();
        int color = (hash & 1) == 0 ? NeonPalette.VIOLET_HOT : NeonPalette.CYAN;
        float cell = AVATAR / 6F, gap = cell * 0.18F, ox = x + cell / 2F, oy = y + cell / 2F;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                if (((hash >>> (row * 3 + col + 1)) & 1) == 0) continue;
                HudDraw.rect(g, ox + col * cell + gap, oy + row * cell + gap, ox + (col + 1) * cell - gap, oy + (row + 1) * cell - gap, f.c(color, alpha));
                HudDraw.rect(g, ox + (4 - col) * cell + gap, oy + row * cell + gap, ox + (5 - col) * cell - gap, oy + (row + 1) * cell - gap, f.c(color, alpha));
            }
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
