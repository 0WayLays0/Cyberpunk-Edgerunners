package com.waylays.chromeneon.client.hud.nav;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.draw.HudScissor;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The minimap itself: terrain composed into a texture every frame from {@link TerrainSampler} (one texel per GUI
 * pixel, so it shares the HUD's pixel grid), then vector overlays — creatures, markers, world spawn, last death,
 * scan pings, the north mark and the player's arrow with its field of view. The frame is cut at two corners.
 */
public final class MinimapDisplay {
    private static final ResourceLocation TEXTURE = ChromeNeon.id("minimap");
    static final float CUT_TL = 10F, CUT_BR = 6F;
    private DynamicTexture texture;
    private int textureSize;

    public static float cellSizeFor(float zoom) {
        return Math.max(1, (int) Math.floor(zoom));
    }

    /** Blocks around the player the map can show (its corners, when it turns). */
    public static float radius() {
        return ClientConfig.MINIMAP_SIZE.get() / 2F * ClientConfig.MINIMAP_ZOOM.get().floatValue() * 1.42F + 4F;
    }

    public void draw(GuiGraphics g, HudFrame f, LocalPlayer player, float x, float y, int size, float boot) {
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 p = player.getPosition(partial);
        MapView view = new MapView(p.x, p.z, player.getViewYRot(partial), ClientConfig.MINIMAP_ROTATE.get(),
                ClientConfig.MINIMAP_ZOOM.get().floatValue(), size);
        compose(f, view, size, boot);

        float[] frame = HudDraw.cutBox(x - 1, y - 1, size + 2, size + 2, CUT_TL + 1, 0, CUT_BR + 1, 0);
        HudDraw.polygon(g, frame, f.c(NeonPalette.VOID, 0.55F));
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1F, 1F, 1F, f.opacity);
        g.blit(TEXTURE, Math.round(x), Math.round(y), 0, 0, size, size, size, size);
        g.setColor(1F, 1F, 1F, 1F);

        if (boot >= 1F) {
            HudScissor.push(g, x, y, x + size, y + size);
            overlays(g, f, player, view, x, y, size, partial);
            HudScissor.pop(g);
            arrow(g, f, view, x + size / 2F, y + size / 2F);
            north(g, f, view, x, y, size);
        } else {
            float line = y + size * boot;
            HudDraw.rect(g, x, line, x + size, line + 1, f.c(NeonPalette.CYAN, 0.9F));
        }
        HudDraw.outline(g, frame, 1F, f.accent(0.9F));
        if (!f.minimal) {
            HudDraw.rect(g, x + CUT_TL + 2, y - 3, x + size * 0.48F, y - 1, f.accent());
            for (float tx = x + 4; tx < x + size - CUT_BR - 4; tx += 6) {
                HudDraw.rect(g, tx, y + size + 1, tx + 1, y + size + 3, f.accent(0.4F));
            }
        }
    }

    /** Writes this frame's terrain into the texture: rotated, masked to the frame, scanlines, darker edges. */
    private void compose(HudFrame f, MapView view, int size, float boot) {
        if (texture == null || textureSize != size) {
            if (texture != null) texture.close();
            texture = new DynamicTexture(size, size, false);
            textureSize = size;
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
        }
        NativeImage img = texture.getPixels();
        if (img == null) return;
        TerrainSampler terrain = ChromeHud.terrain();
        float half = size / 2F;
        int revealed = Math.round(size * boot);
        double[] axes = view.axes();
        double rx = axes[0], rz = axes[1], ux = axes[2], uz = axes[3];
        for (int py = 0; py < size; py++) {
            double dy = py + 0.5 - half;
            double rowX = view.centerX() - ux * dy, rowZ = view.centerZ() - uz * dy;
            for (int px = 0; px < size; px++) {
                if (py >= revealed || !inside(px, py, size)) {
                    img.setPixelRGBA(px, py, 0);
                    continue;
                }
                double dx = px + 0.5 - half;
                int c = terrain.colorAt(rowX + rx * dx, rowZ + rz * dx);
                if (c == 0) c = ((px + py) % 5 == 0) ? MapPalette.NO_DATA_LINE : MapPalette.NO_DATA;
                float k = (py & 1) == 1 ? 0.9F : 1F;
                float edge = Math.min(Math.min(px, py), Math.min(size - 1 - px, size - 1 - py));
                if (edge < 5) k *= 0.6F + 0.08F * edge;
                c = MapPalette.scale(c, k);
                img.setPixelRGBA(px, py, abgr(c, 0.9F));
            }
        }
        texture.upload();
    }

    static boolean inside(float px, float py, int size) {
        return px + py >= CUT_TL && (size - 1 - px) + (size - 1 - py) >= CUT_BR;
    }

    private static int abgr(int argb, float alpha) {
        int a = Math.round(255 * alpha);
        return (a << 24) | ((argb & 0xFF) << 16) | (argb & 0xFF00) | ((argb >> 16) & 0xFF);
    }

    private void overlays(GuiGraphics g, HudFrame f, LocalPlayer player, MapView view, float x, float y, int size, float partial) {
        float cx = x + size / 2F, cy = y + size / 2F;
        ClientLevel level = Minecraft.getInstance().level;
        // Field of view: a faint cone ahead of the player
        float fov = (float) Math.toRadians(38), reach = size * 0.45F;
        float base = view.rotate() ? 0F : (float) Math.toRadians(player.getViewYRot(partial) + 180F);
        float lx = cx + (float) Math.sin(base - fov) * reach, ly = cy - (float) Math.cos(base - fov) * reach;
        float rx = cx + (float) Math.sin(base + fov) * reach, ry = cy - (float) Math.cos(base + fov) * reach;
        HudDraw.quad(g, cx, cy, lx, ly, rx, ry, cx, cy, f.c(NeonPalette.CYAN, 0.22F),
                f.c(NeonPalette.CYAN, 0F), f.c(NeonPalette.CYAN, 0F), f.c(NeonPalette.CYAN, 0.22F));

        for (MapPings.Ping ping : ChromeHud.pings().active(f.now)) {
            float t = (f.now - ping.start()) / MapPings.LIFE;
            float[] m = view.toMap(ping.pos().x, ping.pos().z);
            float r = Math.max(2F, ping.radius() / view.zoom()) * Easing.outCubic(t) + 2F;
            HudDraw.arc(g, cx + m[0], cy + m[1], r, 1F, 0F, (float) (Math.PI * 2), 28, f.c(ping.color(), 1F - t));
        }

        if (ClientConfig.MINIMAP_ENTITIES.get()) {
            double range = size / 2F * view.zoom() * 1.42F;
            int drawn = 0;
            for (Entity e : level.entitiesForRendering()) {
                if (!(e instanceof LivingEntity living) || e == player || !e.isAlive() || drawn > 96) continue;
                Vec3 ep = e.getPosition(partial);
                if (Math.abs(ep.x - view.centerX()) > range || Math.abs(ep.z - view.centerZ()) > range) continue;
                float[] m = view.toMap(ep.x, ep.z);
                float ex = cx + m[0], ey = cy + m[1];
                if (!inside(ex - x, ey - y, size)) continue;
                drawn++;
                entityDot(g, f, living, ex, ey, ep.y - player.getY());
            }
        }

        if (ClientConfig.MINIMAP_MARKERS.get()) {
            String dim = level.dimension().location().toString();
            BlockPos spawn = level.getSharedSpawnPos();
            if (level.dimensionType().natural()) poi(g, f, view, x, y, size, spawn.getX() + 0.5, spawn.getZ() + 0.5, NeonPalette.VIOLET_HOT, "S");
            Optional<GlobalPos> death = player.getLastDeathLocation();
            if (death.isPresent() && death.get().dimension().equals(level.dimension())) {
                BlockPos d = death.get().pos();
                poi(g, f, view, x, y, size, d.getX() + 0.5, d.getZ() + 0.5, NeonPalette.RED, "X");
            }
            for (MapMarkers.Marker marker : ChromeHud.markers().in(dim)) {
                poi(g, f, view, x, y, size, marker.pos().getX() + 0.5, marker.pos().getZ() + 0.5, NeonPalette.CYAN,
                        String.valueOf(marker.number()));
            }
        }
    }

    /** Hostiles orange squares, players white diamonds, other creatures small cyan dots; a tick shows height. */
    private static void entityDot(GuiGraphics g, HudFrame f, LivingEntity e, float x, float y, double dy) {
        int color;
        float r;
        if (e instanceof Player) {
            color = NeonPalette.WHITE;
            r = 2F;
            HudDraw.quad(g, x, y - r - 1, x - r - 1, y, x, y + r + 1, x + r + 1, y, f.c(NeonPalette.VOID, 0.8F));
            HudDraw.quad(g, x, y - r, x - r, y, x, y + r, x + r, y, f.c(color));
        } else {
            boolean hostile = e instanceof Enemy;
            color = hostile ? NeonPalette.ORANGE : NeonPalette.CYAN_SOFT;
            r = hostile ? 1.5F : 1F;
            HudDraw.rect(g, x - r - 0.5F, y - r - 0.5F, x + r + 0.5F, y + r + 0.5F, f.c(NeonPalette.VOID, 0.8F));
            HudDraw.rect(g, x - r, y - r, x + r, y + r, f.c(color, hostile ? 1F : 0.8F));
        }
        if (dy > 3) HudDraw.rect(g, x - 1, y - r - 2.5F, x + 1, y - r - 1.5F, f.c(color, 0.8F));
        else if (dy < -3) HudDraw.rect(g, x - 1, y + r + 1.5F, x + 1, y + r + 2.5F, f.c(color, 0.8F));
    }

    /** A point of interest: a diamond with its letter; outside the map it sticks to the edge, pointing out. */
    private static void poi(GuiGraphics g, HudFrame f, MapView view, float x, float y, int size, double wx, double wz,
                            int color, String label) {
        float half = size / 2F, inset = half - 5F;
        float[] m = view.toMap(wx, wz);
        float dx = m[0], dy = m[1];
        boolean outside = Math.abs(dx) > inset || Math.abs(dy) > inset;
        if (outside) {
            float k = inset / Math.max(Math.abs(dx), Math.abs(dy));
            dx *= k;
            dy *= k;
        }
        float px = x + half + dx, py = y + half + dy;
        HudDraw.quad(g, px, py - 4, px - 4, py, px, py + 4, px + 4, py, f.c(NeonPalette.VOID, 0.85F));
        HudDraw.quad(g, px, py - 3, px - 3, py, px, py + 3, px + 3, py, f.c(color));
        var text = HudText.micro(label);
        HudText.draw(g, text, px - HudText.width(text) / 2F + 0.5F, py - 2, f.c(NeonPalette.VOID));
        if (outside) {
            float len = (float) Math.sqrt(dx * dx + dy * dy), ux = dx / len, uy = dy / len;
            HudDraw.triangle(g, px + ux * 7, py + uy * 7, px + ux * 4 - uy * 2.5F, py + uy * 4 + ux * 2.5F,
                    px + ux * 4 + uy * 2.5F, py + uy * 4 - ux * 2.5F, f.c(color));
        }
    }

    /** The player: a white arrowhead over a dark one, pointing where they look. */
    private static void arrow(GuiGraphics g, HudFrame f, MapView view, float cx, float cy) {
        float a = view.rotate() ? 0F : (float) Math.toRadians(view.yawDegrees() + 180F);
        drawArrow(g, cx, cy, a, 5.5F, f.c(NeonPalette.VOID, 0.9F));
        drawArrow(g, cx, cy, a, 4F, f.c(NeonPalette.WHITE));
    }

    private static void drawArrow(GuiGraphics g, float cx, float cy, float a, float s, int c) {
        float sin = (float) Math.sin(a), cos = (float) Math.cos(a);
        float tipX = cx + sin * s, tipY = cy - cos * s;
        float lX = cx + (-0.8F * s) * cos - (-0.75F * s) * sin, lY = cy + (-0.8F * s) * sin + (-0.75F * s) * cos;
        float rX = cx + (0.8F * s) * cos - (-0.75F * s) * sin, rY = cy + (0.8F * s) * sin + (-0.75F * s) * cos;
        float nX = cx - sin * (-0.25F * s), nY = cy + cos * (-0.25F * s);
        HudDraw.triangle(g, tipX, tipY, lX, lY, nX, nY, c);
        HudDraw.triangle(g, tipX, tipY, nX, nY, rX, rY, c);
    }

    /** "N" where north meets the frame. */
    private static void north(GuiGraphics g, HudFrame f, MapView view, float x, float y, int size) {
        float[] m = view.toMap(view.centerX(), view.centerZ() - 1000);
        float half = size / 2F, inset = half - 4F;
        float k = inset / Math.max(Math.abs(m[0]), Math.abs(m[1]));
        float px = x + half + m[0] * k, py = y + half + m[1] * k;
        var n = HudText.micro("N");
        HudDraw.rect(g, px - 3, py - 3.5F, px + 3, py + 3.5F, f.c(NeonPalette.VOID, 0.9F));
        HudText.draw(g, n, px - 1.5F, py - 2.5F, f.c(NeonPalette.CYAN));
    }
}
