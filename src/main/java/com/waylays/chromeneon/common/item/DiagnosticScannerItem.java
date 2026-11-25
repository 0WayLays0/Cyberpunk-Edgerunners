package com.waylays.chromeneon.common.item;

import com.waylays.chromeneon.common.hud.HudMessages;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.player.CyberReport;
import com.waylays.chromeneon.common.player.CyberStats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A handheld diagnostic unit: the holder's cyberware state from the server's data, in chat and on the HUD. */
public class DiagnosticScannerItem extends Item {
    private static final int COOLDOWN_TICKS = 20;

    public DiagnosticScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            CyberReport.of(server).forEach(line -> server.sendSystemMessage(line));
            CyberStats stats = CyberData.stats(server);
            HudMessages.notify(server, NotificationKind.SCAN, Component.translatable("chromeneon.notify.diagnostic.title"),
                    Component.translatable("chromeneon.notify.diagnostic.message", stats.humanity(), stats.maxHumanity(),
                            stats.ram(), stats.ramCapacity()));
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4F, 1.8F);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
