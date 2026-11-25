package com.waylays.chromeneon.client;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.command.HudCommand;
import com.waylays.chromeneon.client.cyberware.BladeLayer;
import com.waylays.chromeneon.client.cyberware.ChairRenderer;
import com.waylays.chromeneon.client.cyberware.ClientCyberware;
import com.waylays.chromeneon.client.cyberware.CyberwareFeedback;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudLayers;
import com.waylays.chromeneon.client.hud.notify.Notification;
import com.waylays.chromeneon.client.input.ChromeKeys;
import com.waylays.chromeneon.client.screen.RipperdocScreen;
import com.waylays.chromeneon.common.network.ClientBridge;
import com.waylays.chromeneon.common.registry.ModBlockEntities;
import com.waylays.chromeneon.common.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Client entry point, loaded only on the physical client: HUD, keys, renderers, screens and payload handlers. */
@Mod(value = ChromeNeon.MODID, dist = Dist.CLIENT)
public final class ChromeNeonClient {
    public ChromeNeonClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(HudLayers::register);
        modBus.addListener(ChromeKeys::register);
        modBus.addListener(ChromeNeonClient::renderers);
        modBus.addListener(ChromeNeonClient::layers);
        NeoForge.EVENT_BUS.addListener(HudCommand::register);
        ClientBridge.notifications = payload -> ChromeHud.notify(Notification.of(payload));
        ClientBridge.feedback = CyberwareFeedback::accept;
        ClientBridge.visuals = ClientCyberware::acceptVisuals;
        ClientBridge.openRipperdoc = payload -> Minecraft.getInstance().setScreen(new RipperdocScreen(payload.chair()));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RIPPERDOC_CHAIR.get(), ChairRenderer::new);
    }

    private static void layers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            if (event.getSkin(model) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new BladeLayer(renderer));
            }
        }
    }
}
