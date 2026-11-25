package com.waylays.chromeneon.client.hud;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.fx.ScreenFxLayer;
import com.waylays.chromeneon.client.hud.nav.NavigationGroup;
import com.waylays.chromeneon.client.hud.notify.CallGroup;
import com.waylays.chromeneon.client.hud.notify.NotificationsGroup;
import com.waylays.chromeneon.client.hud.scan.TargetOverlay;
import com.waylays.chromeneon.client.hud.status.StatusGroup;
import com.waylays.chromeneon.client.hud.vitals.VitalsGroup;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Where each part of the HUD sits among the game's GUI layers. Screen effects go under everything (they distort
 * the world, not the interface); targeting sits with the crosshair; panels take the place of the vanilla health,
 * armor and effects; notifications and calls sit under the chat so typing is never hidden.
 */
public final class HudLayers {
    private HudLayers() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerBelowAll(ChromeNeon.id("screen_fx"), new ScreenFxLayer());
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ChromeNeon.id("target"), new TargetOverlay());
        event.registerAbove(ChromeNeon.id("target"), ChromeNeon.id("optics"), new com.waylays.chromeneon.client.cyberware.OpticsOverlay());
        event.registerAbove(VanillaGuiLayers.EFFECTS, ChromeNeon.id("vitals"), new VitalsGroup());
        event.registerAbove(ChromeNeon.id("vitals"), ChromeNeon.id("status"), new StatusGroup());
        event.registerAbove(ChromeNeon.id("status"), ChromeNeon.id("navigation"), new NavigationGroup());
        event.registerAbove(ChromeNeon.id("navigation"), ChromeNeon.id("implants"), new ImplantsGroup());
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ChromeNeon.id("notifications"), new NotificationsGroup(ChromeHud.notifications()));
        event.registerAbove(ChromeNeon.id("notifications"), ChromeNeon.id("call"), new CallGroup());
    }
}
