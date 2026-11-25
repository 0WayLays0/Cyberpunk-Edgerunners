package com.waylays.chromeneon.common.cyberware.implants;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.mechanics.TimeDilation;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Leg implant: a second jump in mid-air ({@link com.waylays.chromeneon.common.cyberware.mechanics.AirJump}). Its
 * charges are the jumps left before landing; they refill on the ground, one more while time is dilated.
 */
public final class SpringTendons extends Implant {
    public SpringTendons() {
        super(Properties.of(Mode.AUTOMATIC, 10, 5, ModBodySlots.LEGS));
    }

    @Override
    public List<Component> describe() {
        return List.of(ImplantText.line("spring_tendons", 0, ServerConfig.EXTRA_JUMPS.get()),
                ImplantText.line("spring_tendons", 1), ImplantText.line("spring_tendons", 2));
    }

    public static int jumpsAllowed(ImplantContext ctx) {
        return ServerConfig.EXTRA_JUMPS.get() + (TimeDilation.isDilating(ctx.player()) ? 1 : 0);
    }

    @Override
    public void tick(ImplantContext ctx) {
        if (ctx.player().onGround() || ctx.player().isInWater() || ctx.player().onClimbable()) ctx.state().charges = jumpsAllowed(ctx);
    }
}
