package com.waylays.chromeneon.client.hud.scan;

import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.network.chat.Component;

/** The classification shown on a target, with its colour. Hostile is the only class in the danger colour. */
public enum ScanClass {
    HOSTILE(NeonPalette.ORANGE),
    NEUTRAL(NeonPalette.VIOLET_HOT),
    PASSIVE(NeonPalette.CYAN),
    PLAYER(NeonPalette.WHITE),
    OBJECT(NeonPalette.STEEL),
    STRUCTURE(NeonPalette.CYAN_SOFT),
    DEVICE(NeonPalette.VIOLET_HOT),
    CONTAINER(NeonPalette.CYAN);

    private final int color;

    ScanClass(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    public Component label() {
        return Component.translatable("chromeneon.scan.class." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
