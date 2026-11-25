package com.waylays.chromeneon.client.hud.scan.providers;

import com.waylays.chromeneon.client.hud.scan.ScanClass;
import com.waylays.chromeneon.client.hud.scan.ScanInfo;
import com.waylays.chromeneon.client.hud.scan.ScanSubject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Blocks that store items: classified as containers, with their capacity. */
public final class ContainerScanProvider extends BlockScanProvider {
    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean supports(ScanSubject subject, ClientLevel level) {
        return subject instanceof ScanSubject.BlockSubject b && level.getBlockEntity(b.pos()) instanceof Container;
    }

    @Override
    public ScanInfo describe(ScanSubject subject, ClientLevel level, Player viewer) {
        ScanSubject.BlockSubject b = (ScanSubject.BlockSubject) subject;
        Container container = (Container) level.getBlockEntity(b.pos());
        List<ScanInfo.Line> lines = new java.util.ArrayList<>();
        lines.add(Lines.line("capacity", String.valueOf(container.getContainerSize())));
        lines.addAll(material(b, level));
        return new ScanInfo(b.state().getBlock().getName(), ScanClass.CONTAINER, designation(b), 0, Float.NaN, lines, 20);
    }
}
