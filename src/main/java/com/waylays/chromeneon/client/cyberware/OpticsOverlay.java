package com.waylays.chromeneon.client.cyberware;

import com.waylays.chromeneon.client.debug.HudPerf;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudVisibility;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What the optics draw over the world: the zoom reticle (a graduated ring, the magnification, the range to what is
 * aimed at, the heading) and the tactical analysis (a marker over every creature in range, through walls, coloured
 * by what it is, with its distance).
 */
public final class OpticsOverlay implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        HudFrame f = HudFrame.current();
        if (f == null || !HudVisibility.panels()) return;
        long start = System.nanoTime();
        if (ClientCyberware.analysing()) tactical(g, f, deltaTracker.getGameTimeDeltaPartialTick(false));
        if (OpticsClient.magnification() > 1.02F) reticle(g, f);
        HudPerf.record("optics", System.nanoTime() - start);
    }

    private static void reticle(GuiGraphics g, HudFrame f) {
        float px = f.screenPx, m = OpticsClient.magnification();
        float in = Easing.clamp((m - 1F) / 0.8F);
        float cx = f.width / 2F, cy = f.height / 2F, r = Math.min(f.width, f.height) * 0.34F;
        int c = f.c(NeonPalette.CYAN, 0.7F * in);
        // Darken outside the ring
        HudDraw.arc(g, cx, cy, r + 2F, Math.max(f.width, f.height), 0F, (float) (Math.PI * 2), 64, f.c(NeonPalette.VOID, 0.45F * in));
        HudDraw.ring(g, cx, cy, r, px, 96, c);
        for (int i = 0; i < 72; i++) {
            float a = (float) Math.toRadians(i * 5), len = i % 18 == 0 ? 8F : i % 6 == 0 ? 4F : 2F;
            float s = (float) Math.sin(a), co = (float) -Math.cos(a);
            HudDraw.line(g, cx + s * (r - len), cy + co * (r - len), cx + s * r, cy + co * r, px, f.c(NeonPalette.CYAN, (i % 6 == 0 ? 0.8F : 0.35F) * in));
        }
        float gap = 6F, arm = r * 0.4F;
        HudDraw.rect(g, cx - arm, cy, cx - gap, cy + px, c);
        HudDraw.rect(g, cx + gap, cy, cx + arm, cy + px, c);
        HudDraw.rect(g, cx, cy + gap, cx + px, cy + arm, c);
        HudDraw.diamond(g, cx, cy, 1.2F, f.c(NeonPalette.WHITE, in));
        Minecraft mc = Minecraft.getInstance();
        HitResult hit = mc.player.pick(256.0, 1F, false);
        double range = hit.getType() == HitResult.Type.MISS ? -1 : hit.getLocation().distanceTo(mc.player.getEyePosition());
        Component mag = HudText.value(String.format(java.util.Locale.ROOT, "X%.1f", m));
        HudText.draw(g, mag, cx + r * 0.72F, cy - r * 0.72F, f.c(NeonPalette.WHITE, in), px);
        Component dist = HudText.data(range < 0 ? "RANGE ---" : String.format(java.util.Locale.ROOT, "RANGE %.1fM", range));
        HudText.draw(g, dist, cx + gap + 4F, cy + 4F, f.c(NeonPalette.CYAN, in), px);
        int heading = Math.floorMod(Math.round(mc.player.getYRot() + 180F), 360);
        Component hd = HudText.data(String.format("HDG %03d", heading));
        HudText.draw(g, hd, cx - HudText.width(hd) / 2F, cy - r - 10F, f.c(NeonPalette.LAVENDER, in), px);
        Component label = HudText.label("ARGUS // OPTICAL ZOOM");
        HudText.draw(g, label, cx - r * 0.95F, cy + r * 0.78F, f.c(NeonPalette.CYAN_SOFT, 0.8F * in), px);
    }

    private static void tactical(GuiGraphics g, HudFrame f, float partial) {
        Minecraft mc = Minecraft.getInstance();
        float px = f.screenPx;
        double range = ServerConfig.TACTICAL_RANGE.get();
        int shown = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living) || e == mc.player || !e.isAlive() || shown > 48) continue;
            double d = e.distanceTo(mc.player);
            if (d > range) continue;
            Vec3 head = e.getPosition(partial).add(0, e.getBbHeight() + 0.35, 0);
            float[] p = ChromeHud.projection().project(head.x, head.y, head.z, f.width, f.height);
            if (p == null || p[0] < 0 || p[1] < 0 || p[0] > f.width || p[1] > f.height) continue;
            shown++;
            int color = living instanceof Player ? NeonPalette.WHITE : living instanceof Enemy ? NeonPalette.ORANGE : NeonPalette.CYAN;
            float pulse = 0.65F + 0.35F * Easing.pulse(f.now + e.getId(), 30F);
            HudDraw.diamond(g, p[0], p[1], 3F, f.c(NeonPalette.VOID, 0.6F));
            HudDraw.outline(g, new float[] {p[0], p[1] - 3F, p[0] + 3F, p[1], p[0], p[1] + 3F, p[0] - 3F, p[1]}, px * 1.5F, f.c(color, pulse));
            HudDraw.rect(g, p[0], p[1] + 3F, p[0] + px, p[1] + 7F, f.c(color, 0.5F));
            Component dist = HudText.label(String.format(java.util.Locale.ROOT, "%.0fM", d));
            HudText.draw(g, dist, p[0] + 4.5F, p[1] - 2F, f.c(color, 0.9F), px);
        }
        var state = ClientCyberware.state(ModImplants.ARGUS_OPTICS.get()).orElse(null);
        float left = state == null ? 0F : Math.max(0F, (state.activeUntil - ClientCyberware.now()) / 20F);
        Component label = HudText.label(ClientCyberware.dilating() ? "TACTICAL ANALYSIS // CHRONOS LINK"
                : String.format(java.util.Locale.ROOT, "TACTICAL ANALYSIS // %.1fS", left));
        HudText.draw(g, label, (f.width - HudText.width(label)) / 2F, 34F, f.c(NeonPalette.CYAN, 0.9F), px);
    }
}
