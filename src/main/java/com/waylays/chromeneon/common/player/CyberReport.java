package com.waylays.chromeneon.common.player;

import com.waylays.chromeneon.common.cyberware.BodySlot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** A readable report of a body's cyberware, shared by the diagnostic scanner and the status command. */
public final class CyberReport {
    private CyberReport() {}

    public static List<Component> of(ServerPlayer player) {
        PlayerCyberData data = CyberData.get(player);
        CyberStats stats = CyberData.stats(player);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("chromeneon.report.header", player.getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("chromeneon.report.humanity", stats.humanity(), stats.maxHumanity())
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("chromeneon.report.ram", stats.ram(), stats.ramCapacity())
                .withStyle(ChatFormatting.AQUA));
        for (BodySlot slot : BodySlot.values()) {
            List<ResourceLocation> ids = data.loadout().in(slot);
            Component content = ids.isEmpty()
                    ? Component.translatable("chromeneon.report.empty").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.literal(String.join(", ", ids.stream()
                            .map(id -> data.isEnabled(id) ? id.toString() : id + " (off)").toList()));
            lines.add(Component.translatable("chromeneon.report.slot", Component.translatable(slot.translationKey()),
                    ids.size(), slot.sockets(), content).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
