package com.waylays.chromeneon.client.hud;

import com.waylays.chromeneon.client.debug.HudPerf;
import com.waylays.chromeneon.common.config.ClientConfig;
import com.waylays.chromeneon.common.config.HudAnchor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * A positioned block of the HUD (vitals, navigation, ...): one GUI layer, placed by its config (anchor, offset,
 * scale), drawn in its own local coordinates. Subclasses only measure and draw; placement, glitch jitter, visibility
 * and timing are handled here once.
 */
public abstract class HudGroup implements LayeredDraw.Layer {
    protected final ClientConfig.Element config;
    private final String name;
    private final int bootDelay;
    private final int seed;

    protected HudGroup(String name, ClientConfig.Element config, int bootDelay) {
        this.name = name;
        this.config = config;
        this.bootDelay = bootDelay;
        this.seed = name.hashCode();
    }

    /** Whether there is anything to show this frame (besides the config switch). */
    protected boolean active(HudFrame f) {
        return true;
    }

    protected abstract float width(HudFrame f);

    protected abstract float height(HudFrame f);

    /** Draws in local coordinates: (0, 0) is the group's top-left corner, before scaling. */
    protected abstract void draw(GuiGraphics g, HudFrame f);

    public HudAnchor anchor() {
        return config.anchor().get();
    }

    protected float boot(HudFrame f) {
        return f.boot(bootDelay);
    }

    @Override
    public final void render(GuiGraphics g, DeltaTracker deltaTracker) {
        HudFrame f = HudFrame.current();
        if (f == null || !HudVisibility.panels() || !config.enabled().get() || !active(f)) return;
        long start = System.nanoTime();
        float w = width(f), h = height(f), s = f.scale;
        HudAnchor anchor = anchor();
        // Offsets grow with the HUD scale, so a scaled HUD keeps the same arrangement
        float x = Math.round(anchor.x(f.width, w * s, config.x().get() * s));
        float y = Math.round(anchor.y(f.height, h * s, config.y().get() * s));
        HudBounds.put(name, x, y, w * s, h * s);
        x += ChromeHud.glitch().jitter(seed, f.now);
        y += ChromeHud.glitch().jitter(seed * 31, f.now) * 0.5F;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(s, s, 1F);
        draw(g, f);
        g.pose().popPose();
        HudPerf.record(name, System.nanoTime() - start);
    }
}
