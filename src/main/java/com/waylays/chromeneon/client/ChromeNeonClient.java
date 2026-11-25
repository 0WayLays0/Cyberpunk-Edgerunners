package com.waylays.chromeneon.client;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.RamCellsLayer;
import com.waylays.chromeneon.common.player.SyncedCyberState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/** Client entry point, loaded only on the physical client: HUD layers, config screen, client-side state reset. */
@Mod(value = ChromeNeon.MODID, dist = Dist.CLIENT)
public final class ChromeNeonClient {
    public ChromeNeonClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(ChromeNeonClient::registerLayers);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> SyncedCyberState.clear());
    }

    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ChromeNeon.id("ram_cells"), new RamCellsLayer());
    }
}
