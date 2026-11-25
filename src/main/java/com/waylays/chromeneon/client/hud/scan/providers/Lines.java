package com.waylays.chromeneon.client.hud.scan.providers;

import com.waylays.chromeneon.client.hud.scan.ScanInfo;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Shared helpers for building scan data. */
final class Lines {
    private Lines() {}

    static ScanInfo.Line line(String key, Component value) {
        return new ScanInfo.Line(Component.translatable("chromeneon.scan.line." + key), value, NeonPalette.CYAN_SOFT);
    }

    static ScanInfo.Line line(String key, Component value, int color) {
        return new ScanInfo.Line(Component.translatable("chromeneon.scan.line." + key), value, color);
    }

    static ScanInfo.Line line(String key, String value) {
        return line(key, Component.literal(value));
    }

    /** A stable short code derived from an id, e.g. {@code 3FA2-ZOM}. */
    static String designation(UUID id, String path) {
        String code = path.replaceAll("[^a-z]", "").toUpperCase(java.util.Locale.ROOT);
        code = code.length() >= 3 ? code.substring(0, 3) : (code + "XXX").substring(0, 3);
        return String.format("%04X-%s", (id.hashCode() >>> 16) & 0xFFFF, code);
    }

    static String designation(long seed, String path) {
        return designation(new UUID(seed, seed * 31), path);
    }
}
