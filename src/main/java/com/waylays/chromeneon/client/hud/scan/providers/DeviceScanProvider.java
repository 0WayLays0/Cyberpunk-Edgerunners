package com.waylays.chromeneon.client.hud.scan.providers;

import com.waylays.chromeneon.client.hud.scan.ScanClass;
import com.waylays.chromeneon.client.hud.scan.ScanInfo;
import com.waylays.chromeneon.client.hud.scan.ScanSubject;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Devices: blocks with a working state the world exposes (open, powered, lit, triggered, enabled): doors,
 * trapdoors, levers, buttons, lamps, furnaces, hoppers... Their state and redstone input are real readings; these
 * are the systems later phases will let the player compromise.
 */
public final class DeviceScanProvider extends BlockScanProvider {
    @Override
    public int priority() {
        return 8;
    }

    @Override
    public boolean supports(ScanSubject subject, ClientLevel level) {
        if (!(subject instanceof ScanSubject.BlockSubject b)) return false;
        BlockState s = b.state();
        return s.hasProperty(BlockStateProperties.OPEN) || s.hasProperty(BlockStateProperties.POWERED)
                || s.hasProperty(BlockStateProperties.LIT) || s.hasProperty(BlockStateProperties.TRIGGERED)
                || s.hasProperty(BlockStateProperties.ENABLED);
    }

    @Override
    public ScanInfo describe(ScanSubject subject, ClientLevel level, Player viewer) {
        ScanSubject.BlockSubject b = (ScanSubject.BlockSubject) subject;
        BlockState s = b.state();
        List<ScanInfo.Line> lines = new ArrayList<>();
        if (s.hasProperty(BlockStateProperties.OPEN)) {
            boolean open = s.getValue(BlockStateProperties.OPEN);
            lines.add(Lines.line("state", Component.translatable(open ? "chromeneon.scan.open" : "chromeneon.scan.closed"),
                    open ? NeonPalette.CYAN : NeonPalette.LAVENDER));
        }
        for (var property : List.of(BlockStateProperties.POWERED, BlockStateProperties.LIT,
                BlockStateProperties.TRIGGERED, BlockStateProperties.ENABLED)) {
            if (s.hasProperty(property)) {
                boolean on = s.getValue(property);
                lines.add(Lines.line("power", Component.translatable(on ? "chromeneon.scan.on" : "chromeneon.scan.off"),
                        on ? NeonPalette.CYAN : NeonPalette.LAVENDER));
                break;
            }
        }
        lines.add(Lines.line("signal", String.valueOf(level.getBestNeighborSignal(b.pos()))));
        lines.add(Lines.line("access", Component.translatable("chromeneon.scan.access.local"), NeonPalette.LAVENDER));
        lines.addAll(material(b, level));
        return new ScanInfo(s.getBlock().getName(), ScanClass.DEVICE, designation(b), 0, Float.NaN, lines, 25);
    }
}
