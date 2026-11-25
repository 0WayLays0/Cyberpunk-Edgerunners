package com.waylays.chromeneon.common.item;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.hud.HudMessages;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.player.PlayerCyberData;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A neural matrix: integrated, it permanently raises the body's cyberware capacity, up to the configured limit.
 * How the capacity grows through play.
 */
public class NeuralMatrixItem extends Item {
    public static final int CAPACITY = 2;

    public NeuralMatrixItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            PlayerCyberData data = CyberData.get(sp);
            if (!data.addCapacity(CAPACITY, ServerConfig.CAPACITY_BONUS_MAX.get())) {
                HudMessages.notify(sp, NotificationKind.WARNING, Component.translatable("chromeneon.notify.matrix.full"), Component.empty());
                return InteractionResultHolder.fail(stack);
            }
            stack.consume(1, player);
            level.playSound(null, player.blockPosition(), ModSounds.IMPLANT_INSTALL.get(), SoundSource.PLAYERS, 0.8F, 1.3F);
            HudMessages.notify(sp, NotificationKind.CYBERWARE, Component.translatable("chromeneon.notify.matrix.title"),
                    Component.translatable("chromeneon.notify.matrix.message", CyberData.stats(sp).capacity()));
            CyberData.sync(sp);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("chromeneon.tooltip.matrix", CAPACITY).withStyle(ChatFormatting.AQUA));
    }
}
