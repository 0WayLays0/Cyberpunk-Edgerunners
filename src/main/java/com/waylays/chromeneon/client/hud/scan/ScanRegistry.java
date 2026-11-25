package com.waylays.chromeneon.client.hud.scan;

import com.waylays.chromeneon.client.hud.scan.providers.BlockScanProvider;
import com.waylays.chromeneon.client.hud.scan.providers.ContainerScanProvider;
import com.waylays.chromeneon.client.hud.scan.providers.DeviceScanProvider;
import com.waylays.chromeneon.client.hud.scan.providers.EntityScanProvider;
import com.waylays.chromeneon.client.hud.scan.providers.LivingScanProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The scan providers, highest priority first. Every target has at least the generic entity or block provider. */
public final class ScanRegistry {
    private final List<ScanProvider> providers = new ArrayList<>();

    public ScanRegistry() {
        register(new EntityScanProvider());
        register(new LivingScanProvider());
        register(new BlockScanProvider());
        register(new ContainerScanProvider());
        register(new DeviceScanProvider());
    }

    public void register(ScanProvider provider) {
        providers.add(provider);
        providers.sort(Comparator.comparingInt(ScanProvider::priority).reversed());
    }

    public ScanInfo describe(ScanSubject subject, ClientLevel level, Player viewer) {
        for (ScanProvider p : providers) {
            if (p.supports(subject, level)) return p.describe(subject, level, viewer);
        }
        throw new IllegalStateException("No scan provider for " + subject);
    }
}
