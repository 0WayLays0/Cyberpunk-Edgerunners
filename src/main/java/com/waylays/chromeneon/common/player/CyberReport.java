package com.waylays.chromeneon.common.player;

import com.waylays.chromeneon.common.cyberware.BodySlot;
import com.waylays.chromeneon.common.cyberware.InstalledImplant;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        long now = player.level().getGameTime();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("chromeneon.report.header", player.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("chromeneon.report.humanity", stats.humanity(), stats.maxHumanity()).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("chromeneon.report.capacity", stats.capacityUsed(), stats.capacity()).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("chromeneon.report.ram", stats.ram(), stats.ramCapacity()).withStyle(ChatFormatting.AQUA));
        for (var entry : ModBodySlots.REGISTRY.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            BodySlot slot = entry.getValue();
            List<InstalledImplant> in = data.loadout().in(id);
            MutableComponent content = Component.empty();
            if (in.isEmpty()) content.append(Component.translatable("chromeneon.report.empty").withStyle(ChatFormatting.DARK_GRAY));
            for (int i = 0; i < in.size(); i++) {
                if (i > 0) content.append(", ");
                content.append(Component.translatable("chromeneon.implant." + in.get(i).id().getPath()));
                if (!in.get(i).state().working(now)) content.append(Component.literal(" (off)").withStyle(ChatFormatting.DARK_GRAY));
            }
            lines.add(Component.translatable("chromeneon.report.slot", Component.translatable(BodySlot.translationKey(id)),
                    in.size(), slot.sockets(), content).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
