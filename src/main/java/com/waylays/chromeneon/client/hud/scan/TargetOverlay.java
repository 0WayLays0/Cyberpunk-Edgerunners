package com.waylays.chromeneon.client.hud.scan;

import com.waylays.chromeneon.client.debug.HudPerf;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudBounds;
import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudVisibility;
import com.waylays.chromeneon.client.hud.anim.Easing;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Targeting on screen: brackets that snap onto the target's real projected bounds, a leader line to a data panel
 * (classification, name, distance, threat), the scan progress under the brackets, then the data typing itself in
 * once the scan completes. While the scan key is held, the screen corners frame the view and a sweep runs down.
 */
public final class TargetOverlay implements LayeredDraw.Layer {
    private static final float PANEL_W = 118F;

    @Override
    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        HudFrame f = HudFrame.current();
        if (f == null || !HudVisibility.panels()) return;
        long start = System.nanoTime();
        TargetTracker t = ChromeHud.target();
        scanMode(g, f, t);
        if (t.subject() != null && t.info() != null && (t.scanning() || ClientConfig.TARGET_BRACKETS.get()
                || !Float.isNaN(t.completedAt()) && f.now - t.completedAt() < 60F)) {
            float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
            float[] rect = ChromeHud.projection().box(bounds(t.subject(), partial), f.width, f.height);
            if (rect != null) target(g, f, t, rect, partial);
        }
        HudPerf.record("target", System.nanoTime() - start);
    }

    private static AABB bounds(ScanSubject subject, float partial) {
        if (subject instanceof ScanSubject.EntitySubject e) {
            Entity entity = e.entity();
            return entity.getBoundingBox().move(entity.getPosition(partial).subtract(entity.position()));
        }
        ScanSubject.BlockSubject b = (ScanSubject.BlockSubject) subject;
        VoxelShape shape = b.state().getShape(Minecraft.getInstance().level, b.pos());
        return (shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds()).move(b.pos());
    }

    private static void scanMode(GuiGraphics g, HudFrame f, TargetTracker t) {
        float sweep = (f.now - t.scanPressedAt()) / 12F;
        if (sweep >= 0F && sweep <= 1F) {
            float y = f.height * Easing.outCubic(sweep);
            HudDraw.vGradient(g, 0, y - 24, f.width, y, NeonPalette.alpha(NeonPalette.CYAN, 0F),
                    NeonPalette.alpha(NeonPalette.CYAN, 0.12F * f.opacity));
            HudDraw.rect(g, 0, y, f.width, y + 1, NeonPalette.alpha(NeonPalette.CYAN, 0.6F * f.opacity * (1F - sweep)));
        }
        if (!t.scanning()) return;
        float in = Easing.outCubic(Easing.progress(f.now, t.scanPressedAt(), 6F));
        float m = 6F + (1F - in) * 10F, len = 14F;
        HudDraw.brackets(g, m, m, f.width - m, f.height - m, len, 1F, f.c(NeonPalette.CYAN, 0.45F * in));
        Component label = HudText.micro(Component.translatable("chromeneon.scan.mode"));
        HudText.draw(g, label, (f.width - HudText.width(label)) / 2F, f.height / 2F + 10F, f.c(NeonPalette.CYAN, 0.8F * in));
    }

    private static void target(GuiGraphics g, HudFrame f, TargetTracker t, float[] r, float partial) {
        ScanInfo info = t.info();
        int color = info.kind().color();
        float acquire = Easing.outCubic(Easing.progress(f.now, t.acquiredAt(), 7F));
        float pad = 3F + (1F - acquire) * 14F;
        float x0 = r[0] - pad, y0 = r[1] - pad, x1 = r[2] + pad, y1 = r[3] + pad;
        if (x1 - x0 < 12F) {
            float c = (x0 + x1) / 2F;
            x0 = c - 6F;
            x1 = c + 6F;
        }
        if (y1 - y0 < 12F) {
            float c = (y0 + y1) / 2F;
            y0 = c - 6F;
            y1 = c + 6F;
        }
        boolean complete = !Float.isNaN(t.completedAt());
        float len = Math.clamp(Math.min(x1 - x0, y1 - y0) * 0.22F, 3F, 9F);
        HudDraw.brackets(g, x0, y0, x1, y1, len, 1F, f.c(color, acquire));
        if (complete) {
            HudDraw.rect(g, x0 - 1, y0 - 1, x0 + 2, y0 + 2, f.c(color));
            HudDraw.rect(g, x1 - 2, y1 - 2, x1 + 1, y1 + 1, f.c(color));
        }
        if (t.scanning() && !complete) {
            int segs = 10;
            float sw = (x1 - x0 - 2 * len) / segs;
            for (int i = 0; i < segs; i++) {
                boolean on = (i + 0.5F) / segs <= t.progress();
                HudDraw.rect(g, x0 + len + i * sw + 0.5F, y1 + 2, x0 + len + (i + 1) * sw - 0.5F, y1 + 3.5F,
                        f.c(color, on ? 0.95F : 0.2F));
            }
        }
        panel(g, f, t, info, x0, y0, x1, y1, color, acquire, complete, partial);
    }

    private static void panel(GuiGraphics g, HudFrame f, TargetTracker t, ScanInfo info, float x0, float y0, float x1,
                              float y1, int color, float acquire, boolean complete, float partial) {
        boolean detailed = t.scanning() || complete;
        float revealT = complete ? f.now - t.completedAt() : -1F;
        List<ScanInfo.Line> lines = complete ? info.lines() : List.of();
        float h = 30F + (t.scanning() && !complete ? 10F : 0F) + (complete ? 4F + lines.size() * 7F + (Float.isNaN(info.health()) ? 0F : 6F) : 0F);
        float w = PANEL_W;
        float py = Math.clamp(y0 - 2F, 4F, f.height - h - 4F);
        // To the right of the target, unless that leaves the screen or runs into a HUD group
        HudBounds.Rect right = new HudBounds.Rect(x1 + 12F, py, x1 + 12F + w, py + h);
        boolean left = right.x1() > f.width - 4F
                || HudBounds.previous().values().stream().anyMatch(right::intersects);
        float px = left ? x0 - 12F - w : x1 + 12F;
        float slide = (1F - acquire) * (left ? -8F : 8F);
        px += slide;

        float cornerX = left ? x0 : x1, joinX = left ? px + w + 2F : px - 2F;
        HudDraw.line(g, cornerX, y0, joinX - (left ? -4F : 4F), py + 5F, 1F, f.c(color, 0.6F * acquire));
        HudDraw.hLine(g, joinX - (left ? -4F : 4F), joinX, py + 5F, f.c(color, 0.6F * acquire));

        HudDraw.cutPanel(g, px, py, w, h, 4, 0, 4, 0, f.c(NeonPalette.VOID, (detailed ? 0.62F : 0.45F) * acquire));
        HudDraw.rect(g, px, py + 4, px + 1, py + h, f.c(color, acquire));
        HudDraw.hLine(g, px + 4, px + w, py, f.c(color, 0.5F * acquire));

        Component chip = HudText.micro(info.kind().label());
        float cw = HudText.width(chip) + 7F;
        HudDraw.parallelogram(g, px + 4, py + 3, cw, 7, 2, f.c(color, acquire));
        HudText.draw(g, chip, px + 7.5F, py + 4, f.c(NeonPalette.VOID, acquire));
        HudText.drawRight(g, HudText.micro(info.designation()), px + w - 4, py + 4, f.c(color, 0.7F * acquire));

        Component name = HudText.hud(truncate(info.name().getString(), 18));
        HudText.drawGlow(g, name, px + 4, py + 12, f.c(NeonPalette.WHITE, acquire), detailed ? 1F : 0F);

        Entity viewer = Minecraft.getInstance().player;
        Vec3 eye = viewer.getEyePosition(partial);
        double dist = t.subject() instanceof ScanSubject.EntitySubject e ? e.entity().getPosition(partial).distanceTo(eye)
                : Vec3.atCenterOf(((ScanSubject.BlockSubject) t.subject()).pos()).distanceTo(eye);
        HudText.draw(g, HudText.micro(String.format(java.util.Locale.ROOT, "DIST %.1fM", dist)), px + 4, py + 22, f.c(NeonPalette.CYAN_SOFT, acquire));
        for (int i = 0; i < 3; i++) {
            float tx = px + w - 22 + i * 6;
            boolean on = i < info.threat();
            HudDraw.triangle(g, tx + 2, py + 22, tx, py + 26, tx + 4, py + 26,
                    f.c(on ? (info.threat() >= 2 ? NeonPalette.ORANGE : NeonPalette.CYAN) : NeonPalette.LAVENDER, on ? acquire : 0.25F * acquire));
        }

        float y = py + 30F;
        if (t.scanning() && !complete) {
            HudIcon.SCAN.draw(g, px + 4, y - 1, f.c(NeonPalette.CYAN, Easing.blink(f.now, 8F) ? 1F : 0.5F));
            Component pct = HudText.micro(Component.translatable("chromeneon.scan.progress", Math.round(t.progress() * 100)));
            HudText.draw(g, pct, px + 16, y + 1, f.c(NeonPalette.CYAN));
        }
        if (complete) {
            if (revealT < 40F) {
                Component ok = HudText.micro(Component.translatable("chromeneon.scan.complete"));
                HudText.drawRight(g, ok, px + w - 28, py + 22, f.c(NeonPalette.CYAN, 1F - revealT / 40F));
            }
            y += 2F;
            if (!Float.isNaN(info.health())) {
                int segs = 12;
                float sw = (w - 10F) / segs;
                for (int i = 0; i < segs; i++) {
                    boolean on = (i + 0.5F) / segs <= info.health();
                    HudDraw.parallelogram(g, px + 4 + i * sw, y, sw - 1.5F, 3, 1, f.c(on ? color : NeonPalette.LAVENDER, on ? 0.9F : 0.2F));
                }
                y += 6F;
            }
            for (int i = 0; i < lines.size(); i++) {
                float lt = (revealT - i * 3F) / 8F;
                if (lt <= 0F) break;
                ScanInfo.Line line = lines.get(i);
                HudText.draw(g, HudText.micro(line.label()), px + 4, y, f.c(NeonPalette.LAVENDER, 0.7F));
                String value = HudText.micro(line.value()).getString();
                HudText.draw(g, HudText.micro(HudText.reveal(value, lt)), px + 44, y, f.c(line.color()));
                y += 7F;
            }
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
