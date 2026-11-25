package com.waylays.chromeneon.client.hud.nav;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudGroup;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * Top-right: the round map in its compass, a line dropping from it to the clock, coordinates under. Either the
 * map or the clock can be turned off.
 */
public final class NavigationGroup extends HudGroup {
    private final MinimapDisplay map = new MinimapDisplay();
    private final ClockDisplay clock = new ClockDisplay();

    public NavigationGroup() {
        super("navigation", ClientConfig.NAVIGATION, 10);
    }

    private static boolean mapOn() {
        return ClientConfig.MINIMAP_ENABLED.get();
    }

    private static boolean clockOn() {
        return ClientConfig.CLOCK_ENABLED.get();
    }

    @Override
    protected boolean active(HudFrame f) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && !p.isSpectator() && (mapOn() || clockOn());
    }

    @Override
    protected float width(HudFrame f) {
        return mapOn() ? ClientConfig.MINIMAP_SIZE.get() + 2 * MinimapDisplay.MARGIN : 64F;
    }

    @Override
    protected float height(HudFrame f) {
        float h = 0F;
        if (mapOn()) h += ClientConfig.MINIMAP_SIZE.get() + 2 * MinimapDisplay.MARGIN;
        if (clockOn()) h += clock.height(f) + (f.minimal ? 0F : 7F);
        return h;
    }

    @Override
    protected void draw(GuiGraphics g, HudFrame f) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        float px = f.px, boot = boot(f), w = width(f), y = 0F;
        if (mapOn()) {
            int size = ClientConfig.MINIMAP_SIZE.get();
            float cx = w / 2F, cy = MinimapDisplay.MARGIN + size / 2F;
            map.draw(g, f, player, cx, cy, size, boot);
            y = size + 2 * MinimapDisplay.MARGIN;
            if (!f.minimal) {
                String zoom = "X" + String.format(java.util.Locale.ROOT, "%.1f", 1F / ClientConfig.MINIMAP_ZOOM.get().floatValue());
                HudText.draw(g, HudText.label("NAV"), 0, 0, f.c(NeonPalette.CYAN_SOFT, 0.75F * boot), px);
                HudText.drawRight(g, HudText.label(zoom), w, 0, f.c(NeonPalette.LAVENDER, 0.6F * boot), px);
                HudDraw.rect(g, w - 2F, y - 6F, w - 2F + px, y + 1F, f.accent(0.5F * boot));
            }
        }
        if (clockOn()) {
            float x0 = mapOn() ? 6F : 0F;
            clock.draw(g, f, mc.level, x0, y, w - 2F - x0, f.boot(14));
            y += clock.height(f);
            if (!f.minimal) {
                String coords = "X " + player.getBlockX() + "  Y " + player.getBlockY() + "  Z " + player.getBlockZ();
                HudText.drawRight(g, HudText.data(coords), w - 2F, y + 1F, f.c(NeonPalette.LAVENDER, 0.8F * f.boot(14)), px);
            }
        }
    }
}
