package com.waylays.chromeneon.client.cyberware;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.anim.Smoothed;
import com.waylays.chromeneon.client.input.ChromeKeys;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * The optics' zoom: holding the zoom key narrows the real field of view (x2 at first, the mouse wheel steps it up
 * to the configured maximum), easing in and out. Only with the optics online.
 */
@EventBusSubscriber(modid = ChromeNeon.MODID, value = Dist.CLIENT)
public final class OpticsClient {
    private static final Smoothed ZOOM = new Smoothed();
    private static float level = 2F;
    private static boolean wasZooming;

    private OpticsClient() {}

    public static boolean zooming() {
        return ChromeKeys.ZOOM.isDown() && ClientCyberware.opticsOnline() && Minecraft.getInstance().screen == null;
    }

    /** Current magnification, smoothed (1 when not zooming). */
    public static float magnification() {
        return Math.max(1F, ZOOM.get());
    }

    public static float level() {
        return level;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        boolean zooming = zooming();
        if (zooming && !wasZooming) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.OPTICS_ZOOM.get(), 1.2F, 0.5F));
        }
        if (!zooming) level = 2F;
        wasZooming = zooming;
        ZOOM.update(zooming ? level : 1F, 1F, 0.45F);
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float m = magnification();
        if (m > 1.001F) event.setNewFovModifier(event.getNewFovModifier() / m);
    }

    /** The wheel steps the zoom while it is held, instead of changing the hotbar slot. */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!zooming()) return;
        float max = ServerConfig.OPTICS_MAX_ZOOM.get().floatValue();
        level = Math.clamp(level * (event.getScrollDeltaY() > 0 ? 1.25F : 0.8F), 1.5F, max);
        event.setCanceled(true);
    }
}
