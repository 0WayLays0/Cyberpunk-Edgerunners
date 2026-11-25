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
 * The incoming call popup: caller's face (or a pattern generated from their name), name, encrypted channel, a
 * live waveform while it rings, and the two answer keys as they are bound. It slides in with a short glitch and
 * leaves flashing the answer's colour.
 */
public final class CallGroup extends HudGroup {
    private static final float W = 150F, H = 57F, AVATAR = 24F;

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
        int ring = ringing ? NeonPalette.lerp(NeonPalette.VIOLET, NeonPalette.CYAN, Easing.pulse(f.now, 20F))
                : calls.outcome() == CallCenter.Outcome.ACCEPTED ? NeonPalette.CYAN : NeonPalette.RED;

        HudDraw.cutPanel(g, x, 0, W, H, 8, 0, 6, 0, f.c(NeonPalette.VOID, 0.8F * alpha));
        HudDraw.cutFrame(g, x, 0, W, H, 8, 0, 6, 0, f.c(ring, 0.9F * alpha));
        HudDraw.rect(g, x + 10, -2, x + 60, 0, f.c(ring, alpha));

        HudIcon.CALL.draw(g, x + 8, 4, f.c(NeonPalette.CYAN, (Easing.blink(f.now, 10F) || !ringing ? 1F : 0.35F) * alpha));
        Component header = HudText.micro(Component.translatable(ringing ? "chromeneon.hud.call.incoming"
                : calls.outcome() == CallCenter.Outcome.ACCEPTED ? "chromeneon.hud.call.connected" : "chromeneon.hud.call.ended"));
        HudText.draw(g, header, x + 20, 6, f.c(NeonPalette.CYAN, alpha));
        int seconds = (int) (age / 20F);
        HudText.drawRight(g, HudText.micro(String.format("%02d:%02d", seconds / 60, seconds % 60)), x + W - 6, 6, f.c(NeonPalette.LAVENDER, alpha));

        float ax = x + 7, ay = 15;
        HudDraw.cutPanel(g, ax - 1, ay - 1, AVATAR + 2, AVATAR + 2, 3, 0, 3, 0, f.c(ring, alpha));
        avatar(g, f, call, ax, ay, alpha);

        HudText.draw(g, HudText.hud(truncate(call.caller().getString(), 16)), x + 37, 17, f.c(NeonPalette.WHITE, alpha));
        HudText.draw(g, HudText.micro(call.channel()), x + 37, 27, f.c(NeonPalette.LAVENDER, 0.75F * alpha));
        for (int i = 0; i < 18; i++) {
            float level = ringing ? 0.25F + 0.75F * Math.abs((float) Math.sin(f.now * 0.35F + i * 0.9F) * (float) Math.cos(f.now * 0.13F + i * 0.4F)) : 0.12F;
            float bh = 1F + 4F * level;
            HudDraw.rect(g, x + 37 + i * 3, 40 - bh, x + 39 + i * 3, 40, f.c(NeonPalette.CYAN, 0.75F * alpha));
        }

        key(g, f, x + 7, 45, ChromeKeys.ACCEPT_CALL.getTranslatedKeyMessage(), Component.translatable("chromeneon.hud.call.accept"),
                NeonPalette.CYAN, alpha * (ringing || calls.outcome() == CallCenter.Outcome.ACCEPTED ? 1F : 0.3F));
        key(g, f, x + 78, 45, ChromeKeys.DECLINE_CALL.getTranslatedKeyMessage(), Component.translatable("chromeneon.hud.call.decline"),
                NeonPalette.RED, alpha * (ringing || calls.outcome() != CallCenter.Outcome.ACCEPTED ? 1F : 0.3F));
    }

    private static void key(GuiGraphics g, HudFrame f, float x, float y, Component key, Component action, int color, float alpha) {
        Component k = HudText.micro(key);
        float kw = HudText.width(k) + 6F;
        HudDraw.parallelogram(g, x, y, kw, 8, 2, f.c(color, alpha));
        HudText.draw(g, k, x + 3.5F, y + 1.5F, f.c(NeonPalette.VOID, alpha));
        HudText.draw(g, HudText.micro(action), x + kw + 5, y + 1.5F, f.c(color, alpha));
    }

    /** The caller's face when they are a player on this server; otherwise a symmetric pattern from their name. */
    private static void avatar(GuiGraphics g, HudFrame f, IncomingCall call, float x, float y, float alpha) {
        var connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = call.playerName() == null || connection == null ? null : connection.getPlayerInfo(call.playerName());
        if (info != null) {
            g.flush();
            g.setColor(1F, 1F, 1F, alpha * f.opacity);
            PlayerFaceRenderer.draw(g, info.getSkin(), Math.round(x), Math.round(y), (int) AVATAR);
            g.setColor(1F, 1F, 1F, 1F);
            return;
        }
        HudDraw.rect(g, x, y, x + AVATAR, y + AVATAR, f.c(NeonPalette.PANEL, alpha));
        int hash = call.caller().getString().hashCode();
        int color = (hash & 1) == 0 ? NeonPalette.VIOLET_HOT : NeonPalette.CYAN;
        float cell = 4F, ox = x + 2, oy = y + 2;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                if (((hash >>> (row * 3 + col + 1)) & 1) == 0) continue;
                HudDraw.rect(g, ox + col * cell, oy + row * cell, ox + (col + 1) * cell, oy + (row + 1) * cell, f.c(color, alpha));
                HudDraw.rect(g, ox + (4 - col) * cell, oy + row * cell, ox + (5 - col) * cell, oy + (row + 1) * cell, f.c(color, alpha));
            }
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
