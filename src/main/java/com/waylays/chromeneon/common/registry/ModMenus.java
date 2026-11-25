package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu types. Empty in phase 1: the cyberware install menu arrives in phase 3. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ChromeNeon.MODID);

    private ModMenus() {}
}
