package com.waylays.chromeneon.common.cyberware.implants;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.UseResult;
import com.waylays.chromeneon.common.cyberware.mechanics.TimeDilation;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * System implant. When triggered, slows every other entity around the player ({@link TimeDilation}) for a few
 * seconds, at the cost of RAM, then cools down. Pressing again ends it early. It is the body's operating system:
 * only one such implant fits.
 */
public final class ChronosAccelerator extends Implant {
    public ChronosAccelerator() {
        super(Properties.of(Mode.TRIGGERED, 25, 8, ModBodySlots.SYSTEM).exclusive("operating_system"));
    }

    @Override
    public List<Component> describe() {
        return List.of(
                ImplantText.line("chronos_accelerator", 0, ServerConfig.ACCEL_SLOWDOWN.get(), ServerConfig.ACCEL_RADIUS.get()),
                ImplantText.line("chronos_accelerator", 1, ImplantText.seconds(ServerConfig.ACCEL_DURATION.get()),
                        ImplantText.seconds(ServerConfig.ACCEL_COOLDOWN.get())),
                ImplantText.line("chronos_accelerator", 2, ServerConfig.ACCEL_RAM.get()),
                ImplantText.line("chronos_accelerator", 3));
    }

    @Override
    public UseResult use(ImplantContext ctx) {
        if (ctx.state().active) {
            end(ctx);
            return UseResult.OK;
        }
        UseResult ready = ready(ctx);
        if (!ready.ok()) return ready;
        if (ctx.state().coolingDown(ctx.now())) return UseResult.COOLDOWN;
        if (ctx.player().isPassenger()) return UseResult.NOT_NOW;
        if (!ctx.data().ram().tryConsume(ServerConfig.ACCEL_RAM.get())) return UseResult.NO_RAM;
        long until = ctx.now() + ServerConfig.ACCEL_DURATION.get();
        ctx.state().setActive(true, ctx.now());
        ctx.state().activeUntil = until;
        TimeDilation.start(ctx.player(), ServerConfig.ACCEL_RADIUS.get(), ServerConfig.ACCEL_SLOWDOWN.get(), until);
        ctx.player().level().playSound(null, ctx.player().blockPosition(), ModSounds.ACCELERATOR_ON.get(), SoundSource.PLAYERS, 1F, 1F);
        return UseResult.OK;
    }

    @Override
    public void tick(ImplantContext ctx) {
        if (!ctx.state().active) return;
        if (ctx.now() >= ctx.state().activeUntil || !ctx.working()) end(ctx);
        else TimeDilation.follow(ctx.player());
    }

    @Override
    public void onStopped(ImplantContext ctx) {
        if (ctx.state().active) end(ctx);
    }

    @Override
    public void onRemoved(ImplantContext ctx) {
        onStopped(ctx);
    }

    private static void end(ImplantContext ctx) {
        ctx.state().setActive(false, ctx.now());
        ctx.state().activeUntil = ctx.now();
        ctx.state().cooldownUntil = ctx.now() + ServerConfig.ACCEL_COOLDOWN.get();
        TimeDilation.stop(ctx.player());
        ctx.player().level().playSound(null, ctx.player().blockPosition(), ModSounds.ACCELERATOR_OFF.get(), SoundSource.PLAYERS, 1F, 1F);
    }
}
