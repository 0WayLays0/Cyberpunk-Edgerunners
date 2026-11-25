package com.waylays.chromeneon.client;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.command.HudCommand;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudLayers;
import com.waylays.chromeneon.client.hud.notify.Notification;
import com.waylays.chromeneon.client.input.ChromeKeys;
import com.waylays.chromeneon.common.hud.ClientHudBridge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Client entry point, loaded only on the physical client: HUD layers, keys, commands, config screen. */
@Mod(value = ChromeNeon.MODID, dist = Dist.CLIENT)
public final class ChromeNeonClient {
    public ChromeNeonClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(HudLayers::register);
        modBus.addListener(ChromeKeys::register);
        NeoForge.EVENT_BUS.addListener(HudCommand::register);
        ClientHudBridge.setNotificationSink(payload -> ChromeHud.notify(Notification.of(payload)));
    }
}
