package com.waylays.chromeneon.client.hud;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.cyberware.ClientCyberware;
import com.waylays.chromeneon.client.hud.nav.MinimapDisplay;
import com.waylays.chromeneon.client.hud.notify.NotificationsGroup;
import com.waylays.chromeneon.client.input.ChromeKeys;
import com.waylays.chromeneon.common.config.ClientConfig;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.player.SyncedCyberState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Drives the HUD from game events: ticks, frames, world projection, keys, joining and leaving, vanilla layers. */
@EventBusSubscriber(modid = ChromeNeon.MODID, value = Dist.CLIENT)
public final class HudEvents {
    private HudEvents() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        HudTime.tick();
        LocalPlayer player = mc.player;
        ChromeHud.vitals().tick(player);
        ChromeHud.ram().tick();
        if (player == null || mc.level == null) return;
        keys(mc, player);
        // Targeting and scanning are the optics' work: without them online, there is no target
        if (ClientCyberware.opticsOnline()) {
            ChromeHud.target().tick(mc, ChromeKeys.SCAN.isDown());
        } else {
            ChromeHud.target().clear();
            if (ChromeKeys.SCAN.consumeClick()) {
                ChromeHud.notify(NotificationKind.WARNING, Component.translatable("chromeneon.hud.optics.required"),
                        Component.translatable("chromeneon.hud.optics.required.message"));
            }
        }
        ChromeHud.calls().tick(ChromeHud.calls().ringing() && ChromeKeys.ACCEPT_CALL.consumeClick(),
                ChromeHud.calls().ringing() && ChromeKeys.DECLINE_CALL.consumeClick());
        int max = ClientConfig.NOTIFICATIONS.enabled().get() ? ClientConfig.NOTIFICATIONS_MAX.get() : 0;
        if (max > 0 && !ClientConfig.NOTIFICATIONS.anchor().get().isBottom()) {
            // On short screens, fewer cards so the stack never reaches the hotbar
            max = Math.min(max, NotificationsGroup.room(mc.getWindow().getGuiScaledHeight(),
                    ClientConfig.HUD_SCALE.get().floatValue(), ClientConfig.NOTIFICATIONS.y().get()));
        }
        ChromeHud.notifications().tick(HudTime.ticks(), max);
        if (ClientConfig.HUD_ENABLED.get() && ClientConfig.MINIMAP_ENABLED.get()) {
            float zoom = ClientConfig.MINIMAP_ZOOM.get().floatValue();
            ChromeHud.terrain().tick(mc.level, player.getX(), player.getY(), player.getZ(), MinimapDisplay.radius(),
                    (int) MinimapDisplay.cellSizeFor(zoom));
        }
    }

    private static void keys(Minecraft mc, LocalPlayer player) {
        while (ChromeKeys.TOGGLE_HUD.consumeClick()) {
            ClientConfig.HUD_ENABLED.set(!ClientConfig.HUD_ENABLED.get());
            ClientConfig.SPEC.save();
            if (ClientConfig.HUD_ENABLED.get()) ChromeHud.boot().start();
        }
        while (ChromeKeys.MARKER.consumeClick()) {
            String dim = mc.level.dimension().location().toString();
            if (player.isShiftKeyDown()) {
                int removed = ChromeHud.markers().removeNearest(dim, player.blockPosition(), 16);
                ChromeHud.notify(removed > 0 ? NotificationKind.INFO : NotificationKind.WARNING,
                        Component.translatable(removed > 0 ? "chromeneon.hud.marker.removed" : "chromeneon.hud.marker.none", removed),
                        Component.empty());
            } else {
                int n = ChromeHud.markers().add(dim, player.blockPosition());
                ChromeHud.notify(n > 0 ? NotificationKind.SCAN : NotificationKind.WARNING,
                        Component.translatable(n > 0 ? "chromeneon.hud.marker.placed" : "chromeneon.hud.marker.full", n),
                        n > 0 ? Component.literal(player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ()) : Component.empty());
                if (n > 0) ChromeHud.ping(player.position(), 4F, com.waylays.chromeneon.client.hud.theme.NeonPalette.CYAN);
            }
        }
        // Answer keys only act while a call rings; otherwise their presses are dropped, not queued
        if (!ChromeHud.calls().ringing()) {
            while (ChromeKeys.ACCEPT_CALL.consumeClick()) { }
            while (ChromeKeys.DECLINE_CALL.consumeClick()) { }
        }
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        HudFrame.begin(event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onLevelStage(RenderLevelStageEvent event) {
        ChromeHud.projection().capture(event);
    }

    /** Our vitals and status replace the vanilla hearts, armor and effect list while they are shown. */
    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        var name = event.getName();
        // The game's "press shift to dismount" line would sit in the middle of the surgery screen
        if (name.equals(VanillaGuiLayers.OVERLAY_MESSAGE) && Minecraft.getInstance().screen instanceof HudVisibility.FullScreenInterface) {
            event.setCanceled(true);
            return;
        }
        if (!HudVisibility.panels()) return;
        if ((name.equals(VanillaGuiLayers.PLAYER_HEALTH) || name.equals(VanillaGuiLayers.ARMOR_LEVEL))
                && ClientConfig.VITALS.enabled().get()) {
            event.setCanceled(true);
        } else if (name.equals(VanillaGuiLayers.EFFECTS) && ClientConfig.STATUS.enabled().get()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ChromeHud.boot().start();
        ChromeHud.markers().load();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SyncedCyberState.clear();
        ClientCyberware.clear();
        com.waylays.chromeneon.client.cyberware.BladeAnimation.clear();
        com.waylays.chromeneon.client.cyberware.Afterimages.clear();
        ChromeHud.reset();
    }
}
