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
    private static final float PANEL_W = 112F;

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
        float px = f.screenPx;
        float sweep = (f.now - t.scanPressedAt()) / 12F;
        if (sweep >= 0F && sweep <= 1F) {
            float y = f.height * Easing.outCubic(sweep);
            HudDraw.vGradient(g, 0, y - 30, f.width, y, NeonPalette.alpha(NeonPalette.CYAN, 0F),
                    NeonPalette.alpha(NeonPalette.CYAN, 0.10F * f.opacity));
            HudDraw.rect(g, 0, y, f.width, y + px, NeonPalette.alpha(NeonPalette.CYAN, 0.7F * f.opacity * (1F - sweep)));
        }
        if (!t.scanning()) return;
        float in = Easing.outCubic(Easing.progress(f.now, t.scanPressedAt(), 6F));
        float m = 10F + (1F - in) * 12F, len = 22F;
        int c = f.c(NeonPalette.CYAN, 0.55F * in);
        HudDraw.brackets(g, m, m, f.width - m, f.height - m, len, px, c);
        HudDraw.brackets(g, m - 3, m - 3, f.width - m + 3, f.height - m + 3, 4F, px * 2, c);
        Component label = HudText.label(Component.translatable("chromeneon.scan.mode"));
        float lw = HudText.width(label);
        float ly = f.height / 2F + 14F;
        HudDraw.rect(g, f.width / 2F - lw / 2F - 10, ly + 1.5F, f.width / 2F - lw / 2F - 3, ly + 1.5F + px, c);
        HudDraw.rect(g, f.width / 2F + lw / 2F + 3, ly + 1.5F, f.width / 2F + lw / 2F + 10, ly + 1.5F + px, c);
        HudText.draw(g, label, (f.width - lw) / 2F, ly, f.c(NeonPalette.CYAN, 0.85F * in), px);
    }

    private static void target(GuiGraphics g, HudFrame f, TargetTracker t, float[] r, float partial) {
        float px = f.screenPx;
        ScanInfo info = t.info();
        int color = info.kind().color();
        float acquire = Easing.outCubic(Easing.progress(f.now, t.acquiredAt(), 7F));
        float pad = 3F + (1F - acquire) * 16F;
        float x0 = r[0] - pad, y0 = r[1] - pad, x1 = r[2] + pad, y1 = r[3] + pad;
        if (x1 - x0 < 14F) {
            float c = (x0 + x1) / 2F;
            x0 = c - 7F;
            x1 = c + 7F;
        }
        if (y1 - y0 < 14F) {
            float c = (y0 + y1) / 2F;
            y0 = c - 7F;
            y1 = c + 7F;
        }
        boolean complete = !Float.isNaN(t.completedAt());
        float len = Math.clamp(Math.min(x1 - x0, y1 - y0) * 0.2F, 3F, 8F);
        HudDraw.brackets(g, x0, y0, x1, y1, len, px, f.c(color, 0.9F * acquire));
        // Heavier accent on the top-left corner, small ticks in the middle of the sides
        HudDraw.rect(g, x0 - px, y0 - px, x0 + 2.5F, y0 + px * 2, f.c(color, acquire));
        HudDraw.rect(g, (x0 + x1) / 2F - 1F, y0 - 1.5F, (x0 + x1) / 2F + 1F, y0 - 1.5F + px, f.c(color, 0.6F * acquire));
        HudDraw.rect(g, (x0 + x1) / 2F - 1F, y1 + 1.5F, (x0 + x1) / 2F + 1F, y1 + 1.5F + px, f.c(color, 0.6F * acquire));
        if (complete) HudDraw.diamond(g, x1, y1, 1.5F, f.c(color));
        if (t.scanning() && !complete) {
            int segs = 12;
            float sw = (x1 - x0) / segs;
            for (int i = 0; i < segs; i++) {
                boolean on = (i + 0.5F) / segs <= t.progress();
                HudDraw.parallelogram(g, x0 + i * sw + 0.3F, y1 + 3F, sw - 0.9F, 1.5F, 0.6F, f.c(color, on ? 0.95F : 0.2F));
            }
        }
        panel(g, f, t, info, x0, y0, x1, y1, color, acquire, complete, partial);
    }

    private static void panel(GuiGraphics g, HudFrame f, TargetTracker t, ScanInfo info, float x0, float y0, float x1,
                              float y1, int color, float acquire, boolean complete, float partial) {
        float px = f.screenPx;
        boolean detailed = t.scanning() || complete;
        float revealT = complete ? f.now - t.completedAt() : -1F;
        List<ScanInfo.Line> lines = complete ? info.lines() : List.of();
        float lineH = 7F;
        float h = 27F + (t.scanning() && !complete ? 9F : 0F) + (complete ? 3F + lines.size() * lineH + (Float.isNaN(info.health()) ? 0F : 6F) : 0F);
        float w = PANEL_W;
        float py = Math.clamp(y0 - 2F, 4F, f.height - h - 4F);
        HudBounds.Rect right = new HudBounds.Rect(x1 + 14F, py, x1 + 14F + w, py + h);
        boolean left = right.x1() > f.width - 4F || HudBounds.previous().values().stream().anyMatch(right::intersects);
        float px0 = (left ? x0 - 14F - w : x1 + 14F) + (1F - acquire) * (left ? -8F : 8F);

        // Leader: from the bracket corner, a diagonal then a horizontal into the panel, with a node
        float cornerX = left ? x0 : x1, joinX = left ? px0 + w : px0;
        float kneeX = cornerX + (left ? -6F : 6F);
        int lead = f.c(color, 0.7F * acquire);
        HudDraw.line(g, cornerX, y0, kneeX, py + 4F, px, lead);
        HudDraw.rect(g, Math.min(kneeX, joinX), py + 4F, Math.max(kneeX, joinX), py + 4F + px, lead);
        HudDraw.diamond(g, joinX, py + 4F + px / 2, 1.3F, lead);

        // Backing: fades away from the leader side, no frame
        int back = f.c(NeonPalette.VOID, (detailed ? 0.55F : 0.4F) * acquire);
        if (left) HudDraw.veil(g, px0 - 10F, py - 3F, px0 + w, py + h + 3F, 5F, back & 0x00FFFFFF, back);
        else HudDraw.veil(g, px0, py - 3F, px0 + w + 10F, py + h + 3F, 5F, back, back & 0x00FFFFFF);
        HudDraw.rect(g, px0, py + 8F, px0 + px * 2, py + h, f.c(color, acquire));

        Component chip = HudText.label(info.kind().label());
        float cw = HudText.width(chip) + 7F;
        HudDraw.parallelogram(g, px0 + 4F, py + 1F, cw, 6F, 1.8F, f.c(color, acquire));
        HudText.drawFlat(g, chip, px0 + 7.5F, py + 1F + (6F - HudText.Kind.LABEL.cap()) / 2F, f.c(NeonPalette.VOID, acquire));
        HudText.draw(g, HudText.label(info.designation()), px0 + cw + 10F, py + 2F, f.c(color, 0.75F * acquire), px);

        Component name = HudText.title(truncate(info.name().getString(), 22));
        HudText.drawGlow(g, name, px0 + 5F, py + 10F, f.c(NeonPalette.WHITE, acquire), detailed ? px : 0F);

        Entity viewer = Minecraft.getInstance().player;
        Vec3 eye = viewer.getEyePosition(partial);
        double dist = t.subject() instanceof ScanSubject.EntitySubject e ? e.entity().getPosition(partial).distanceTo(eye)
                : Vec3.atCenterOf(((ScanSubject.BlockSubject) t.subject()).pos()).distanceTo(eye);
        float ry = py + 20F;
        HudText.draw(g, HudText.data(String.format(java.util.Locale.ROOT, "%.1fM", dist)), px0 + 5F, ry, f.c(NeonPalette.CYAN_SOFT, acquire), px);
        for (int i = 0; i < 3; i++) {
            float tx = px0 + w - 20F + i * 6F;
            boolean on = i < info.threat();
            HudDraw.triangle(g, tx + 2F, ry, tx, ry + 4F, tx + 4F, ry + 4F,
                    f.c(on ? (info.threat() >= 2 ? NeonPalette.ORANGE : NeonPalette.CYAN) : NeonPalette.LAVENDER, on ? acquire : 0.25F * acquire));
        }
        if (complete && revealT < 40F) {
            Component ok = HudText.label(Component.translatable("chromeneon.scan.complete"));
            HudText.drawRight(g, ok, px0 + w - 24F, ry + 0.5F, f.c(NeonPalette.CYAN, 1F - revealT / 40F), px);
        }

        float y = py + 28F;
        if (t.scanning() && !complete) {
            HudIcon.SCAN.draw(g, px0 + 5F, y, 6F, f.c(NeonPalette.CYAN, Easing.blink(f.now, 8F) ? 1F : 0.5F));
            Component pct = HudText.data(Component.translatable("chromeneon.scan.progress", Math.round(t.progress() * 100)));
            HudText.draw(g, pct, px0 + 14F, y + 1F, f.c(NeonPalette.CYAN), px);
        }
        if (complete) {
            if (!Float.isNaN(info.health())) {
                int segs = 20;
                float sw = (w - 14F) / segs;
                for (int i = 0; i < segs; i++) {
                    boolean on = (i + 0.5F) / segs <= info.health();
                    HudDraw.parallelogram(g, px0 + 5F + i * sw, y, sw - 0.8F, 2.5F, 0.8F, f.c(on ? color : NeonPalette.LAVENDER, on ? 0.9F : 0.2F));
                }
                y += 6F;
            }
            for (int i = 0; i < lines.size(); i++) {
                float lt = (revealT - i * 3F) / 8F;
                if (lt <= 0F) break;
                ScanInfo.Line line = lines.get(i);
                Component label = HudText.label(line.label());
                HudText.draw(g, label, px0 + 5F, y + 0.5F, f.c(NeonPalette.LAVENDER, 0.75F), px);
                HudDraw.dashes(g, px0 + 7F + HudText.width(label), px0 + 46F, y + 3F, px * 2, px * 3, px, f.c(NeonPalette.LAVENDER, 0.35F));
                String value = HudText.data(line.value()).getString();
                HudText.draw(g, HudText.data(HudText.reveal(value, lt)), px0 + 48F, y, f.c(line.color()), px);
                y += lineH;
            }
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
