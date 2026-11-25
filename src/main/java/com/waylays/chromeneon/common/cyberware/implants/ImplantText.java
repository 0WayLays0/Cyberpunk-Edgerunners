package com.waylays.chromeneon.common.cyberware.implants;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Shared wording of implant descriptions. */
final class ImplantText {
    private ImplantText() {}

    static Component line(String implant, int n, Object... args) {
        return Component.translatable("chromeneon.implant." + implant + ".desc." + n, args);
    }

    static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20F);
    }
}
