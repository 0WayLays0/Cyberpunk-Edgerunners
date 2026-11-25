package com.waylays.chromeneon.client.hud.scan;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Describes a kind of target. Registered in {@link ScanRegistry}; the provider with the highest priority that
 * supports a target describes it. Later phases add providers for machines, cameras and hackable systems.
 */
public interface ScanProvider {
    int priority();

    boolean supports(ScanSubject subject, ClientLevel level);

    ScanInfo describe(ScanSubject subject, ClientLevel level, Player viewer);
}
