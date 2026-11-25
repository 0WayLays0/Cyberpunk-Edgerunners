package com.waylays.chromeneon.client.hud.nav;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The minimap: a disc of terrain composed every frame from {@link TerrainSampler} and drawn with linear filtering
 * (a smooth tactical map, not blocks), its edge fading out. Around it, a graduated compass ring turning with the
 * view with its cardinal letters, fixed accent arcs and a slowly turning dashed ring. On the map: creatures, markers,
 * world spawn, last death, scan pings, the field of view and the player's arrow.
 */
public final class MinimapDisplay {
    private static final ResourceLocation TEXTURE = ChromeNeon.id("minimap");
    /** Texels per GUI pixel of the map image: finer than the GUI grid, smoothed by linear filtering. */
    private static final int DETAIL = 2;
    /** Room around the disc for the compass ring and its letters. */
    public static final float MARGIN = 13F;
    private DynamicTexture texture;
    private int textureSize;

    public static float cellSizeFor(float zoom) {
        return Math.max(1, (int) Math.floor(zoom));
    }

    /** Blocks around the player the map can show. */
    public static float radius() {
        return ClientConfig.MINIMAP_SIZE.get() / 2F * ClientConfig.MINIMAP_ZOOM.get().floatValue() + 4F;
    }

    /** Draws the map with its centre at ({@code cx}, {@code cy}) and diameter {@code size}. */
    public void draw(GuiGraphics g, HudFrame f, LocalPlayer player, float cx, float cy, int size, float boot) {
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 p = player.getPosition(partial);
        MapView view = new MapView(p.x, p.z, player.getViewYRot(partial), ClientConfig.MINIMAP_ROTATE.get(),
                ClientConfig.MINIMAP_ZOOM.get().floatValue(), size);
        float r = size / 2F, px = f.px;
        float open = Easing.outCubic(boot);
        compose(view, size, open);

        // Dark disc under the map, then the terrain
        HudDraw.arc(g, cx, cy, 0F, r + 1F, 0F, (float) (Math.PI * 2), 48, f.c(NeonPalette.VOID, 0.5F * open));
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1F, 1F, 1F, f.opacity);
        g.pose().pushPose();
        g.pose().translate(cx - r, cy - r, 0);
        g.pose().scale(1F / DETAIL, 1F / DETAIL, 1F);
        int texels = size * DETAIL;
        g.blit(TEXTURE, 0, 0, 0, 0, texels, texels, texels, texels);
        g.pose().popPose();
        g.setColor(1F, 1F, 1F, 1F);

        if (boot >= 1F) overlays(g, f, player, view, cx, cy, r, partial);
        frame(g, f, view, cx, cy, r, open);
        if (boot >= 1F) arrow(g, f, view, cx, cy);
    }

    /** The rings around the map. */
    private static void frame(GuiGraphics g, HudFrame f, MapView view, float cx, float cy, float r, float open) {
        float px = f.px;
        float turn = view.rotate() ? (float) Math.toRadians(-view.yawDegrees() + 180F) : 0F;
        HudDraw.ring(g, cx, cy, r + 1F, px, 64, f.accent(0.55F * open));
        // Compass graduation: every 10 degrees, longer every 30, letters at the cardinal points
        for (int i = 0; i < 36; i++) {
            float a = turn + (float) Math.toRadians(i * 10);
            float len = i % 9 == 0 ? 3.5F : i % 3 == 0 ? 2F : 1F;
            float s = (float) Math.sin(a), c = (float) -Math.cos(a);
            HudDraw.line(g, cx + s * (r + 3F), cy + c * (r + 3F), cx + s * (r + 3F + len), cy + c * (r + 3F + len),
                    i % 9 == 0 ? px * 2 : px, f.c(i % 9 == 0 ? NeonPalette.CYAN : NeonPalette.LAVENDER, (i % 9 == 0 ? 0.9F : 0.45F) * open));
        }
        String[] letters = {"N", "E", "S", "W"};
        for (int i = 0; i < 4; i++) {
            float a = turn + (float) (i * Math.PI / 2);
            float lx = cx + (float) Math.sin(a) * (r + 10F), ly = cy - (float) Math.cos(a) * (r + 10F);
            Component l = HudText.data(letters[i]);
            HudText.draw(g, l, lx - HudText.width(l) / 2F, ly - HudText.Kind.DATA.cap() / 2F,
                    f.c(i == 0 ? NeonPalette.CYAN : NeonPalette.LAVENDER, (i == 0 ? 1F : 0.6F) * open), px);
        }
        // Fixed accents and a slowly turning dashed ring
        HudDraw.arc(g, cx, cy, r + 1F, px * 3F, (float) Math.toRadians(-140), (float) Math.toRadians(-100), 12, f.accent(open));
        HudDraw.arc(g, cx, cy, r + 1F, px * 3F, (float) Math.toRadians(100), (float) Math.toRadians(112), 4, f.c(NeonPalette.CYAN, 0.8F * open));
        HudDraw.dashedRing(g, cx, cy, r * 0.5F, px, 24, 0.45F, f.now * 0.004F, f.c(NeonPalette.WHITE, 0.08F * open));
    }

    /** Writes this frame's terrain: rotated, cut to a disc with a soft edge, darkening toward the rim. */
    private void compose(MapView view, int mapSize, float open) {
        int size = mapSize * DETAIL;
        if (texture == null || textureSize != size) {
            if (texture != null) texture.close();
            texture = new DynamicTexture(size, size, false);
            textureSize = size;
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
            texture.setFilter(true, false);
        }
        NativeImage img = texture.getPixels();
        if (img == null) return;
        TerrainSampler terrain = ChromeHud.terrain();
        float half = size / 2F, visible = half * open;
        double[] axes = view.axes();
        double rx = axes[0] / DETAIL, rz = axes[1] / DETAIL, ux = axes[2] / DETAIL, uz = axes[3] / DETAIL;
        for (int py = 0; py < size; py++) {
            double dy = py + 0.5 - half;
            double rowX = view.centerX() - ux * dy, rowZ = view.centerZ() - uz * dy;
            for (int pxl = 0; pxl < size; pxl++) {
                double dx = pxl + 0.5 - half;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d > visible) {
                    img.setPixelRGBA(pxl, py, 0);
                    continue;
                }
                int c = terrain.colorAt(rowX + rx * dx, rowZ + rz * dx);
                if (c == 0) c = MapPalette.NO_DATA;
                float rim = Math.clamp((visible - d) / (4F * DETAIL), 0F, 1F);
                c = MapPalette.scale(c, 1F - 0.35F * (d / half) * (d / half));
                img.setPixelRGBA(pxl, py, abgr(c, 0.9F * rim));
            }
        }
        texture.upload();
    }

    static boolean inside(float dx, float dy, float r) {
        return dx * dx + dy * dy <= r * r;
    }

    private static int abgr(int argb, float alpha) {
        int a = Math.round(255 * alpha);
        return (a << 24) | ((argb & 0xFF) << 16) | (argb & 0xFF00) | ((argb >> 16) & 0xFF);
    }

    private void overlays(GuiGraphics g, HudFrame f, LocalPlayer player, MapView view, float cx, float cy, float r, float partial) {
        ClientLevel level = Minecraft.getInstance().level;
        // Field of view: a cone fading out ahead of the player
        float fov = (float) Math.toRadians(36), reach = r * 0.85F;
        float base = view.rotate() ? 0F : (float) Math.toRadians(player.getViewYRot(partial) + 180F);
        float lx = cx + (float) Math.sin(base - fov) * reach, ly = cy - (float) Math.cos(base - fov) * reach;
        float rx = cx + (float) Math.sin(base + fov) * reach, ry = cy - (float) Math.cos(base + fov) * reach;
        HudDraw.quad(g, cx, cy, lx, ly, rx, ry, cx, cy, f.c(NeonPalette.CYAN, 0.28F),
                f.c(NeonPalette.CYAN, 0F), f.c(NeonPalette.CYAN, 0F), f.c(NeonPalette.CYAN, 0.28F));

        for (MapPings.Ping ping : ChromeHud.pings().active(f.now)) {
            float t = (f.now - ping.start()) / MapPings.LIFE;
            float[] m = view.toMap(ping.pos().x, ping.pos().z);
            if (!inside(m[0], m[1], r)) continue;
            float pr = Math.min(r - 1F, (Math.max(2F, ping.radius() / view.zoom()) * Easing.outCubic(t) + 2F));
            HudDraw.ring(g, cx + m[0], cy + m[1], pr, f.px * 1.5F, 32, f.c(ping.color(), 1F - t));
        }

        if (ClientConfig.MINIMAP_ENTITIES.get()) {
            double range = r * view.zoom();
            int drawn = 0;
            for (Entity e : level.entitiesForRendering()) {
                if (!(e instanceof LivingEntity living) || e == player || !e.isAlive() || drawn > 96) continue;
                Vec3 ep = e.getPosition(partial);
                if (Math.abs(ep.x - view.centerX()) > range || Math.abs(ep.z - view.centerZ()) > range) continue;
                float[] m = view.toMap(ep.x, ep.z);
                if (!inside(m[0], m[1], r - 2F)) continue;
                drawn++;
                entityDot(g, f, living, cx + m[0], cy + m[1], ep.y - player.getY());
            }
        }

        if (ClientConfig.MINIMAP_MARKERS.get()) {
            String dim = level.dimension().location().toString();
            BlockPos spawn = level.getSharedSpawnPos();
            if (level.dimensionType().natural()) poi(g, f, view, cx, cy, r, spawn.getX() + 0.5, spawn.getZ() + 0.5, NeonPalette.VIOLET_HOT, "S");
            Optional<GlobalPos> death = player.getLastDeathLocation();
            if (death.isPresent() && death.get().dimension().equals(level.dimension())) {
                BlockPos d = death.get().pos();
                poi(g, f, view, cx, cy, r, d.getX() + 0.5, d.getZ() + 0.5, NeonPalette.RED, "X");
            }
            for (MapMarkers.Marker marker : ChromeHud.markers().in(dim)) {
                poi(g, f, view, cx, cy, r, marker.pos().getX() + 0.5, marker.pos().getZ() + 0.5, NeonPalette.CYAN,
                        String.valueOf(marker.number()));
            }
        }
    }

    /** Hostiles are orange diamonds, players white rings, other creatures small cyan dots; a tick shows height. */
    private static void entityDot(GuiGraphics g, HudFrame f, LivingEntity e, float x, float y, double dy) {
        float px = f.px;
        int color;
        if (e instanceof Player) {
            color = NeonPalette.WHITE;
            HudDraw.ring(g, x, y, 1.6F, px * 2, 12, f.c(color));
        } else if (e instanceof Enemy) {
            color = NeonPalette.ORANGE;
            HudDraw.diamond(g, x, y, 1.9F, f.c(NeonPalette.VOID, 0.7F));
            HudDraw.diamond(g, x, y, 1.4F, f.c(color));
        } else {
            color = NeonPalette.CYAN_SOFT;
            HudDraw.arc(g, x, y, 0F, 0.9F, 0F, (float) (Math.PI * 2), 8, f.c(color, 0.85F));
        }
        if (dy > 3) HudDraw.rect(g, x - 1, y - 3.2F, x + 1, y - 3.2F + px * 2, f.c(color, 0.8F));
        else if (dy < -3) HudDraw.rect(g, x - 1, y + 3.2F - px * 2, x + 1, y + 3.2F, f.c(color, 0.8F));
    }

    /** A point of interest: a pin with its letter; beyond the map it sits on the rim, pointing outward. */
    private static void poi(GuiGraphics g, HudFrame f, MapView view, float cx, float cy, float r, double wx, double wz,
                            int color, String label) {
        float[] m = view.toMap(wx, wz);
        float dx = m[0], dy = m[1];
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        boolean outside = d > r - 4F;
        if (outside) {
            dx = dx / d * (r - 4F);
            dy = dy / d * (r - 4F);
        }
        float x = cx + dx, y = cy + dy;
        HudDraw.diamond(g, x, y, 3.4F, f.c(NeonPalette.VOID, 0.8F));
        HudDraw.diamond(g, x, y, 2.8F, f.c(color));
        Component text = HudText.label(label);
        HudText.drawFlat(g, text, x - HudText.width(text) / 2F, y - HudText.Kind.LABEL.cap() / 2F, f.c(NeonPalette.VOID));
        if (outside && d > 0) {
            float ux = dx / (r - 4F), uy = dy / (r - 4F);
            HudDraw.triangle(g, x + ux * 6.5F, y + uy * 6.5F, x + ux * 4F - uy * 2F, y + uy * 4F + ux * 2F,
                    x + ux * 4F + uy * 2F, y + uy * 4F - ux * 2F, f.c(color));
        }
    }

    /** The player: a white arrowhead with a dark edge, pointing where they look. */
    private static void arrow(GuiGraphics g, HudFrame f, MapView view, float cx, float cy) {
        float a = view.rotate() ? 0F : (float) Math.toRadians(view.yawDegrees() + 180F);
        drawArrow(g, cx, cy, a, 4.6F, f.c(NeonPalette.VOID, 0.85F));
        drawArrow(g, cx, cy, a, 3.6F, f.c(NeonPalette.WHITE));
    }

    private static void drawArrow(GuiGraphics g, float cx, float cy, float a, float s, int c) {
        float sin = (float) Math.sin(a), cos = (float) Math.cos(a);
        float tipX = cx + sin * s, tipY = cy - cos * s;
        float lX = cx + (-0.75F * s) * cos - (-0.8F * s) * sin, lY = cy + (-0.75F * s) * sin + (-0.8F * s) * cos;
        float rX = cx + (0.75F * s) * cos - (-0.8F * s) * sin, rY = cy + (0.75F * s) * sin + (-0.8F * s) * cos;
        float nX = cx - sin * (-0.3F * s), nY = cy + cos * (-0.3F * s);
        HudDraw.triangle(g, tipX, tipY, lX, lY, nX, nY, c);
        HudDraw.triangle(g, tipX, tipY, nX, nY, rX, rY, c);
    }
}
