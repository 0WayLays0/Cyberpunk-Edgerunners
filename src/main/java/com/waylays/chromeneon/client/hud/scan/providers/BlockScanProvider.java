package com.waylays.chromeneon.client.hud.scan.providers;

import com.waylays.chromeneon.client.hud.scan.ScanClass;
import com.waylays.chromeneon.client.hud.scan.ScanInfo;
import com.waylays.chromeneon.client.hud.scan.ScanProvider;
import com.waylays.chromeneon.client.hud.scan.ScanSubject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Any block: hardness, the tool it calls for, blast resistance. */
public class BlockScanProvider implements ScanProvider {
    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean supports(ScanSubject subject, ClientLevel level) {
        return subject instanceof ScanSubject.BlockSubject;
    }

    @Override
    public ScanInfo describe(ScanSubject subject, ClientLevel level, Player viewer) {
        ScanSubject.BlockSubject b = (ScanSubject.BlockSubject) subject;
        return new ScanInfo(b.state().getBlock().getName(), ScanClass.STRUCTURE, designation(b), 0, Float.NaN,
                material(b, level), 15);
    }

    static String designation(ScanSubject.BlockSubject b) {
        return Lines.designation(b.pos().asLong(), BuiltInRegistries.BLOCK.getKey(b.state().getBlock()).getPath());
    }

    static List<ScanInfo.Line> material(ScanSubject.BlockSubject b, ClientLevel level) {
        BlockState s = b.state();
        List<ScanInfo.Line> lines = new ArrayList<>();
        float hardness = s.getDestroySpeed(level, b.pos());
        lines.add(Lines.line("hardness", hardness < 0 ? Component.translatable("chromeneon.scan.indestructible")
                : Component.literal(String.format(java.util.Locale.ROOT, "%.1f", hardness))));
        String tool = s.is(BlockTags.MINEABLE_WITH_PICKAXE) ? "pickaxe" : s.is(BlockTags.MINEABLE_WITH_AXE) ? "axe"
                : s.is(BlockTags.MINEABLE_WITH_SHOVEL) ? "shovel" : s.is(BlockTags.MINEABLE_WITH_HOE) ? "hoe" : "hand";
        lines.add(Lines.line("tool", Component.translatable("chromeneon.scan.tool." + tool)));
        lines.add(Lines.line("blast", String.format(java.util.Locale.ROOT, "%.0f", s.getExplosionResistance(level, b.pos(), null))));
        return lines;
    }
}
