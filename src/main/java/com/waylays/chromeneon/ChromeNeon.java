package com.waylays.chromeneon;

import com.mojang.logging.LogUtils;
import com.waylays.chromeneon.common.config.ClientConfig;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.network.ChromeNetwork;
import com.waylays.chromeneon.common.registry.ModAttachments;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModEntities;
import com.waylays.chromeneon.common.registry.ModSounds;
import com.waylays.chromeneon.common.registry.ModBlockEntities;
import com.waylays.chromeneon.common.registry.ModBlocks;
import com.waylays.chromeneon.common.registry.ModCreativeTabs;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.registry.ModItems;
import com.waylays.chromeneon.common.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Entry point of Chrome Néon. Only wires the registries, the configs and the network together: every system lives
 * in its own package (see docs/ARCHITECTURE.md). Client-only code is reached through {@code client.ChromeNeonClient},
 * which is a separate {@code @Mod(dist = CLIENT)} entry point and is never loaded on a dedicated server.
 */
@Mod(ChromeNeon.MODID)
public final class ChromeNeon {
    public static final String MODID = "chromeneon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChromeNeon(IEventBus modBus, ModContainer container) {
        ModBodySlots.register(modBus);
        ModImplants.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(ChromeNetwork::register);

        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
