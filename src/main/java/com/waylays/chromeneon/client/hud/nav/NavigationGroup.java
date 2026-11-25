package com.waylays.chromeneon.client.hud.nav;

import com.waylays.chromeneon.client.hud.HudFrame;
import com.waylays.chromeneon.client.hud.HudGroup;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/** Top-right: navigation header, minimap, clock, coordinates. Either the map or the clock can be turned off. */
public final class NavigationGroup extends HudGroup {
    private static final float HEADER = 8F;
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
        return mapOn() ? ClientConfig.MINIMAP_SIZE.get() : ClockDisplay.width(f);
    }

    @Override
    protected float height(HudFrame f) {
        float h = 0F;
        if (mapOn()) h += (f.minimal ? 0F : HEADER) + ClientConfig.MINIMAP_SIZE.get() + 6F;
        if (clockOn()) h += clock.height(f) + (f.minimal ? 0F : 8F);
        return h;
    }

    @Override
    protected void draw(GuiGraphics g, HudFrame f) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        float boot = boot(f), w = width(f), y = 0F;
        if (mapOn()) {
            int size = ClientConfig.MINIMAP_SIZE.get();
            if (!f.minimal) {
                HudText.draw(g, HudText.micro("NAV.SYS"), 0, 0, f.c(NeonPalette.CYAN_SOFT, 0.75F * boot));
                String zoom = "X" + String.format(java.util.Locale.ROOT, "%.1f", 1F / ClientConfig.MINIMAP_ZOOM.get().floatValue());
                HudText.drawRight(g, HudText.micro(zoom), w, 0, f.c(NeonPalette.LAVENDER, 0.6F * boot));
                y = HEADER;
            }
            map.draw(g, f, player, 0, y, size, boot);
            y += size + 6F;
        }
        if (clockOn()) {
            clock.draw(g, f, mc.level, 0, y, w, f.boot(14));
            y += clock.height(f);
            if (!f.minimal) {
                String coords = "X " + player.getBlockX() + "  Y " + player.getBlockY() + "  Z " + player.getBlockZ();
                HudText.draw(g, HudText.micro(coords), 0, y + 2, f.c(NeonPalette.LAVENDER, 0.75F * f.boot(14)));
            }
        }
    }
}
