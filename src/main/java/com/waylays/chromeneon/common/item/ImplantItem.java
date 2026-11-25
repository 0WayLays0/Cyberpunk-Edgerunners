package com.waylays.chromeneon.common.item;

import com.waylays.chromeneon.common.cyberware.BodySlot;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.registry.ModItems;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implant outside a body: the item carried to a ripperdoc chair and consumed by the installation (removal gives
 * it back). Its tooltip is the implant's technical sheet.
 */
public class ImplantItem extends Item {
    private final Supplier<? extends Implant> implant;

    public ImplantItem(Supplier<? extends Implant> implant, Properties properties) {
        super(properties);
        this.implant = implant;
    }

    public Implant implant() {
        return implant.get();
    }

    /** The item that carries {@code implant}. */
    public static Optional<ItemStack> stackFor(Implant implant) {
        return ModItems.ITEMS.getEntries().stream().map(h -> h.get())
                .filter(i -> i instanceof ImplantItem ii && ii.implant() == implant).findFirst().map(ItemStack::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Implant.Properties p = implant().properties();
        MutableComponent slots = Component.empty();
        boolean first = true;
        for (ResourceLocation slot : p.slots()) {
            if (!first) slots.append(" / ");
            slots.append(Component.translatable(BodySlot.translationKey(slot)));
            first = false;
        }
        tooltip.add(Component.translatable("chromeneon.tooltip.slot", slots).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("chromeneon.tooltip.costs", p.humanityCost(), p.capacityCost()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("chromeneon.tooltip.mode." + p.mode().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.DARK_AQUA));
        for (Component line : implant().describe()) tooltip.add(line.copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("chromeneon.tooltip.install").withStyle(ChatFormatting.DARK_GRAY));
    }

    public ResourceLocation implantId() {
        return ModImplants.id(implant());
    }
}
