package com.waylays.chromeneon.common.cyberware.implants;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.mechanics.Cooldowns;
import com.waylays.chromeneon.common.cyberware.mechanics.CyberMath;
import com.waylays.chromeneon.common.hud.HudMessages;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * Circulatory implant: when a hit leaves the player under the trigger threshold, it injects: half its dose at once,
 * the rest over two seconds. It stores a few injections, refills them one at a time, and waits between two
 * injections. Nothing happens above the threshold: this is an emergency system, not regeneration.
 */
public final class HemostatPump extends Implant {
    static final int SPREAD_TICKS = 40;

    public HemostatPump() {
        super(Properties.of(Mode.AUTOMATIC, 10, 5, ModBodySlots.CIRCULATORY));
    }

    @Override
    public List<Component> describe() {
        return List.of(
                ImplantText.line("hemostat_pump", 0, Math.round(ServerConfig.PUMP_TRIGGER.get() * 100),
                        String.format(java.util.Locale.ROOT, "%.0f", ServerConfig.PUMP_HEAL.get())),
                ImplantText.line("hemostat_pump", 1, ServerConfig.PUMP_CHARGES.get(), ImplantText.seconds(ServerConfig.PUMP_RECHARGE.get())),
                ImplantText.line("hemostat_pump", 2, ImplantText.seconds(ServerConfig.PUMP_COOLDOWN.get())));
    }

    @Override
    public void onInstalled(ImplantContext ctx) {
        ctx.state().charges = ServerConfig.PUMP_CHARGES.get();
    }

    @Override
    public void tick(ImplantContext ctx) {
        int max = ServerConfig.PUMP_CHARGES.get();
        if (ctx.state().charges > max) ctx.state().charges = max;
        if (ctx.state().charges < max && ctx.working()) {
            ctx.state().resource += 1F;
            if (ctx.state().resource >= ServerConfig.PUMP_RECHARGE.get()) {
                ctx.state().resource = 0F;
                ctx.state().charges++;
            }
        }
        if (ctx.state().active) {
            if (ctx.now() >= ctx.state().activeUntil || !ctx.player().isAlive()) ctx.state().setActive(false, ctx.now());
            else ctx.player().heal(ServerConfig.PUMP_HEAL.get().floatValue() / 2F / SPREAD_TICKS);
        }
    }

    /** After a hit: injects if the conditions are met. */
    public boolean afterDamage(ImplantContext ctx) {
        var p = ctx.player();
        if (!ctx.working() || !CyberMath.shouldInject(p.getHealth(), p.getMaxHealth(), ServerConfig.PUMP_TRIGGER.get().floatValue(),
                ctx.state().charges, ctx.state().coolingDown(ctx.now()))) {
            return false;
        }
        ctx.state().charges--;
        ctx.state().cooldownUntil = ctx.now() + Cooldowns.of(ctx, this, ServerConfig.PUMP_COOLDOWN.get());
        ctx.state().setActive(true, ctx.now());
        ctx.state().activeUntil = ctx.now() + SPREAD_TICKS;
        p.heal(ServerConfig.PUMP_HEAL.get().floatValue() / 2F);
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.35, 0.5, 0.35, 0.05);
            level.playSound(null, p.blockPosition(), ModSounds.PUMP_INJECT.get(), SoundSource.PLAYERS, 1F, 1F);
        }
        HudMessages.notify(p, NotificationKind.SYSTEM_ON, Component.translatable("chromeneon.notify.pump.title"),
                Component.translatable("chromeneon.notify.pump.message", ctx.state().charges));
        return true;
    }
}
