package com.waylays.chromeneon.common.cyberware.implants;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.mechanics.CyberMath;
import com.waylays.chromeneon.common.hud.HudMessages;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

import java.util.List;

/**
 * Skin implant: plating under the skin with its own integrity. It takes a share of every physical hit (blows,
 * projectiles, explosions) after vanilla armor, and loses that much integrity. At zero it is breached and protects
 * nothing until it has repaired a quarter of itself; it repairs only after a few seconds without damage. Fire,
 * falls, drowning, magic and anything that bypasses armor go through it.
 */
public final class CarapaceDermis extends Implant {
    public CarapaceDermis() {
        super(Properties.of(Mode.PASSIVE, 15, 6, ModBodySlots.INTEGUMENTARY));
    }

    @Override
    public List<Component> describe() {
        return List.of(
                ImplantText.line("carapace_dermis", 0, Math.round(ServerConfig.DERMIS_SHARE.get() * 100),
                        Math.round(ServerConfig.DERMIS_INTEGRITY.get())),
                ImplantText.line("carapace_dermis", 1, ImplantText.seconds(ServerConfig.DERMIS_REGEN_DELAY.get()),
                        String.format(java.util.Locale.ROOT, "%.1f", ServerConfig.DERMIS_REGEN.get())),
                ImplantText.line("carapace_dermis", 2));
    }

    public static float max() {
        return ServerConfig.DERMIS_INTEGRITY.get().floatValue();
    }

    @Override
    public void onInstalled(ImplantContext ctx) {
        ctx.state().resource = max();
        ctx.state().setActive(true, ctx.now());
    }

    @Override
    public void tick(ImplantContext ctx) {
        float max = max();
        if (ctx.state().resource > max) ctx.state().resource = max;
        if (ctx.now() - ctx.data().lastHurtAt > ServerConfig.DERMIS_REGEN_DELAY.get() && ctx.state().resource < max) {
            ctx.state().resource = Math.min(max, ctx.state().resource + ServerConfig.DERMIS_REGEN.get().floatValue() / 20F);
        }
        if (!ctx.state().active && ctx.state().resource >= max * 0.25F) ctx.state().setActive(true, ctx.now());
    }

    @Override
    public void onStopped(ImplantContext ctx) {
        // A disabled or malfunctioning plating simply stops absorbing; its integrity is kept
    }

    public static boolean physical(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypeTags.IS_FALL)
                && !source.is(DamageTypeTags.IS_DROWNING) && !source.is(DamageTypeTags.IS_FREEZING) && !source.is(DamageTypeTags.IS_LIGHTNING);
    }

    /** Takes its share of a hit; returns the damage left for the body. */
    public float absorb(ImplantContext ctx, DamageSource source, float damage) {
        if (!ctx.working() || !ctx.state().active || !physical(source) || damage <= 0F) return damage;
        float[] r = CyberMath.absorb(damage, ctx.state().resource, ServerConfig.DERMIS_SHARE.get().floatValue());
        float taken = damage - r[0];
        ctx.state().resource = r[1];
        if (taken > 0F && ctx.player().level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, ctx.player().getX(), ctx.player().getY() + 1.0, ctx.player().getZ(),
                    6 + Math.round(taken * 2), 0.3, 0.4, 0.3, 0.2);
            level.playSound(null, ctx.player().blockPosition(), ModSounds.DERMIS_ABSORB.get(), SoundSource.PLAYERS, 0.8F, 0.9F + level.random.nextFloat() * 0.2F);
        }
        if (ctx.state().resource <= 0.01F) {
            ctx.state().resource = 0F;
            ctx.state().setActive(false, ctx.now());
            ctx.player().level().playSound(null, ctx.player().blockPosition(), ModSounds.DERMIS_BREACH.get(), SoundSource.PLAYERS, 1F, 1F);
            HudMessages.notify(ctx.player(), NotificationKind.WARNING, Component.translatable("chromeneon.notify.dermis.breached"),
                    Component.translatable("chromeneon.notify.dermis.breached.message"));
        }
        return r[0];
    }
}
