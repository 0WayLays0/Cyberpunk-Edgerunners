package com.waylays.chromeneon.common.cyberware.implants;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.UseResult;
import com.waylays.chromeneon.common.cyberware.mechanics.Cooldowns;
import com.waylays.chromeneon.common.cyberware.mechanics.TimeDilation;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * The body's operating system for speed. Triggered, it slows every other entity around the player
 * ({@link TimeDilation}) for a few seconds at the cost of RAM, and the player's hits can turn critical while it runs;
 * pressing again ends it early; then it cools down (shorter with the implants that reduce it). Three models trade
 * slowdown, duration, cooldown and critical hits against humanity and capacity. Only one operating system fits.
 */
public final class ChronosAccelerator extends Implant {
    /** Short name of the model on screens (MK.I…). */
    private final String model;
    private final ServerConfig.ChronosModel config;

    public ChronosAccelerator(String model, ServerConfig.ChronosModel config, int humanity, int capacity) {
        super(Properties.of(Mode.TRIGGERED, humanity, capacity, ModBodySlots.SYSTEM).exclusive(OPERATING_SYSTEM));
        this.model = model;
        this.config = config;
    }

    public String model() {
        return model;
    }

    public int duration() {
        return config.duration().get();
    }

    public int baseCooldown() {
        return config.cooldown().get();
    }

    public int slowdown() {
        return config.slowdown().get();
    }

    public double critChance() {
        return config.critChance().get();
    }

    public double critDamage() {
        return config.critDamage().get();
    }

    @Override
    public List<Component> describe() {
        return List.of(
                ImplantText.line("chronos", 0, slowdown(), ServerConfig.ACCEL_RADIUS.get()),
                ImplantText.line("chronos", 1, ImplantText.seconds(duration()), ImplantText.seconds(baseCooldown())),
                ImplantText.line("chronos", 2, Math.round(critChance() * 100), Math.round(critDamage() * 100)),
                ImplantText.line("chronos", 3, ServerConfig.ACCEL_RAM.get()),
                ImplantText.line("chronos", 4));
    }

    /** Whether this copy is running now. */
    public static boolean running(ImplantContext ctx) {
        return ctx.state().active && ctx.now() < ctx.state().activeUntil;
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
        long until = ctx.now() + duration();
        ctx.state().setActive(true, ctx.now());
        ctx.state().activeUntil = until;
        TimeDilation.start(ctx.player(), ServerConfig.ACCEL_RADIUS.get(), slowdown(), until);
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

    private void end(ImplantContext ctx) {
        ctx.state().setActive(false, ctx.now());
        ctx.state().activeUntil = ctx.now();
        ctx.state().cooldownUntil = ctx.now() + Cooldowns.of(ctx, this, baseCooldown());
        TimeDilation.stop(ctx.player());
        ctx.player().level().playSound(null, ctx.player().blockPosition(), ModSounds.ACCELERATOR_OFF.get(), SoundSource.PLAYERS, 1F, 1F);
    }
}
