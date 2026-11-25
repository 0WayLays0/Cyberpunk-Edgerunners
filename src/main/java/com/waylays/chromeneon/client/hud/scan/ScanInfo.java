package com.waylays.chromeneon.client.hud.scan;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * What a scan reveals about a target, built by a {@link ScanProvider} from what the client really knows.
 *
 * @param designation a short identifier shown in the panel's corner
 * @param threat      0 (none) to 3 (high)
 * @param health      0..1, or NaN when the target has no health
 * @param lines       data revealed once the scan completes
 * @param scanTicks   how long the scan takes
 */
public record ScanInfo(Component name, ScanClass kind, String designation, int threat, float health,
                       List<Line> lines, int scanTicks) {
    /** One line of revealed data: a label and its value, in a colour. */
    public record Line(Component label, Component value, int color) {}
}
