package com.waylays.chromeneon.client.hud;

import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.Minecraft;

/** When the HUD is drawn at all. */
public final class HudVisibility {
    private HudVisibility() {}

    /** The HUD is on, there is a player, the GUI is not hidden (F1). */
    public static boolean hud() {
        Minecraft mc = Minecraft.getInstance();
        return ClientConfig.HUD_ENABLED.get() && mc.player != null && mc.level != null && !mc.options.hideGui;
    }

    /**
     * Panels step aside while the debug screen (F3) covers the corners, and under the mod's full-screen interfaces
     * (cyberware, surgery), which show the same data themselves.
     */
    public static boolean panels() {
        Minecraft mc = Minecraft.getInstance();
        return hud() && !mc.getDebugOverlay().showDebugScreen() && !(mc.screen instanceof FullScreenInterface);
    }

    /** Marks a screen that covers the whole HUD. */
    public interface FullScreenInterface {}

    /** Health and armor follow the game's rule: only shown where the player can be hurt. */
    public static boolean vulnerable() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameMode != null && mc.gameMode.canHurtPlayer();
    }
}
