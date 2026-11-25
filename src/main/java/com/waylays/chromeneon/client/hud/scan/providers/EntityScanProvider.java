package com.waylays.chromeneon.client.hud.scan.providers;

import com.waylays.chromeneon.client.hud.scan.ScanClass;
import com.waylays.chromeneon.client.hud.scan.ScanInfo;
import com.waylays.chromeneon.client.hud.scan.ScanProvider;
import com.waylays.chromeneon.client.hud.scan.ScanSubject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Any entity without health (items, vehicles, projectiles...): type, and for dropped items what they hold. */
public final class EntityScanProvider implements ScanProvider {
    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean supports(ScanSubject subject, ClientLevel level) {
        return subject instanceof ScanSubject.EntitySubject;
    }

    @Override
    public ScanInfo describe(ScanSubject subject, ClientLevel level, Player viewer) {
        Entity e = ((ScanSubject.EntitySubject) subject).entity();
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        List<ScanInfo.Line> lines = new ArrayList<>();
        lines.add(Lines.line("type", e.getType().getDescription()));
        if (e instanceof ItemEntity item) {
            lines.add(Lines.line("content", Component.literal(item.getItem().getCount() + "x ").append(item.getItem().getHoverName())));
        }
        lines.add(Lines.line("velocity", String.format(java.util.Locale.ROOT, "%.1f M/S", e.getDeltaMovement().length() * 20)));
        return new ScanInfo(e.getDisplayName(), ScanClass.OBJECT, Lines.designation(e.getUUID(), path), 0, Float.NaN, lines, 15);
    }
}
