package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.item.DiagnosticScannerItem;
import com.waylays.chromeneon.common.item.ImplantItem;
import com.waylays.chromeneon.common.item.NeuralMatrixItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChromeNeon.MODID);

    /** The mod's emblem and the base of every implant's recipe. */
    public static final DeferredItem<Item> NEURAL_CHIP = ITEMS.registerSimpleItem("neural_chip",
            new Item.Properties().rarity(Rarity.UNCOMMON));

    /** Reads the holder's cyberware state from the server-side data. */
    public static final DeferredItem<DiagnosticScannerItem> DIAGNOSTIC_SCANNER = ITEMS.registerItem(
            "diagnostic_scanner", DiagnosticScannerItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<NeuralMatrixItem> NEURAL_MATRIX = ITEMS.registerItem("neural_matrix", NeuralMatrixItem::new,
            new Item.Properties().rarity(Rarity.RARE).stacksTo(16));

    public static final DeferredItem<BlockItem> RIPPERDOC_CHAIR = ITEMS.registerSimpleBlockItem(ModBlocks.RIPPERDOC_CHAIR);

    public static final DeferredItem<ImplantItem> CHRONOS_ACCELERATOR = implant("chronos_accelerator", ModImplants.CHRONOS_ACCELERATOR, Rarity.EPIC);
    public static final DeferredItem<ImplantItem> REAPER_BLADES = implant("reaper_blades", ModImplants.REAPER_BLADES, Rarity.RARE);
    public static final DeferredItem<ImplantItem> SPRING_TENDONS = implant("spring_tendons", ModImplants.SPRING_TENDONS, Rarity.UNCOMMON);
    public static final DeferredItem<ImplantItem> CARAPACE_DERMIS = implant("carapace_dermis", ModImplants.CARAPACE_DERMIS, Rarity.RARE);
    public static final DeferredItem<ImplantItem> HEMOSTAT_PUMP = implant("hemostat_pump", ModImplants.HEMOSTAT_PUMP, Rarity.RARE);
    public static final DeferredItem<ImplantItem> ARGUS_OPTICS = implant("argus_optics", ModImplants.ARGUS_OPTICS, Rarity.UNCOMMON);
    public static final DeferredItem<ImplantItem> SYNAPSE_COPROCESSOR = implant("synapse_coprocessor", ModImplants.SYNAPSE_COPROCESSOR, Rarity.UNCOMMON);

    private ModItems() {}

    private static DeferredItem<ImplantItem> implant(String name, DeferredHolder<Implant, ? extends Implant> implant, Rarity rarity) {
        return ITEMS.registerItem(name, p -> new ImplantItem(implant, p), new Item.Properties().stacksTo(1).rarity(rarity));
    }
}
