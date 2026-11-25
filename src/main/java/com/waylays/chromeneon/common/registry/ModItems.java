package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.item.DiagnosticScannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChromeNeon.MODID);

    /** The mod's emblem: a neural chip, icon of the creative tab and crafting base of future implants. */
    public static final DeferredItem<Item> NEURAL_CHIP = ITEMS.registerSimpleItem("neural_chip",
            new Item.Properties().rarity(Rarity.UNCOMMON));

    /** Reads the holder's cyberware state (humanity, RAM, installed implants) from the server-side data. */
    public static final DeferredItem<DiagnosticScannerItem> DIAGNOSTIC_SCANNER = ITEMS.registerItem(
            "diagnostic_scanner", DiagnosticScannerItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<BlockItem> RIPPERDOC_CHAIR = ITEMS.registerSimpleBlockItem(ModBlocks.RIPPERDOC_CHAIR);

    private ModItems() {}
}
