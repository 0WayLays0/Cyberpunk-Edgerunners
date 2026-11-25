package com.waylays.chromeneon.common.cyberware.mechanics;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.implants.CarapaceDermis;
import com.waylays.chromeneon.common.cyberware.implants.ChronosAccelerator;
import com.waylays.chromeneon.common.cyberware.implants.HemostatPump;
import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Where cyberware meets the damage system: the blades' strikes (extra damage while time is dilated, their own
 * sound), the accelerator's critical hits while it runs, the plating's share of physical hits (after vanilla armor
 * and enchantments), the pump's injection after a hit, and the time of the last hit (no surgery during a fight).
 */
@EventBusSubscriber(modid = ChromeNeon.MODID)
public final class CyberCombatEvents {
    private CyberCombatEvents() {}

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim) {
            CyberData.get(victim).lastHurtAt = victim.level().getGameTime();
        }
        // A blade strike: the attacker hits with an empty hand while the blades are out
        if (event.getSource().getDirectEntity() instanceof ServerPlayer attacker && event.getSource().getEntity() == attacker
                && BladeCombat.armed(attacker) && CyberData.get(attacker).lungeTicks == 0) {
            event.setAmount(event.getAmount() * BladeCombat.damageFactor(attacker));
            attacker.level().playSound(null, event.getEntity().blockPosition(), ModSounds.BLADES_HIT.get(), SoundSource.PLAYERS, 0.9F,
                    0.9F + attacker.getRandom().nextFloat() * 0.2F);
        }
        // While the accelerator runs, the user's hits (melee or their own projectiles) can turn critical
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
            for (var fitted : Cyberware.ofType(attacker, ChronosAccelerator.class)) {
                if (!ChronosAccelerator.running(fitted.ctx())) continue;
                ChronosAccelerator model = fitted.implant();
                float m = CyberMath.critMultiplier(attacker.getRandom().nextDouble(), model.critChance(), model.critDamage());
                if (m > 1F) critical(attacker, event.getEntity(), event, m);
                break;
            }
        }
    }

    private static void critical(ServerPlayer attacker, LivingEntity victim, LivingIncomingDamageEvent event, float multiplier) {
        event.setAmount(event.getAmount() * multiplier);
        if (attacker.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 14, 0.3, 0.3, 0.3, 0.35);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY(0.6), victim.getZ(), 8, 0.25, 0.25, 0.25, 0.2);
            level.playSound(null, victim.blockPosition(), ModSounds.ACCELERATOR_CRIT.get(), SoundSource.PLAYERS, 1F,
                    0.95F + attacker.getRandom().nextFloat() * 0.1F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CarapaceDermis dermis = ModImplants.CARAPACE_DERMIS.get();
        float damage = event.getNewDamage();
        for (ImplantContext ctx : Cyberware.of(player, dermis)) damage = dermis.absorb(ctx, event.getSource(), damage);
        if (damage != event.getNewDamage()) {
            event.setNewDamage(damage);
            CyberData.sync(player);
        }
    }

    @SubscribeEvent
    public static void afterDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        HemostatPump pump = ModImplants.HEMOSTAT_PUMP.get();
        for (ImplantContext ctx : Cyberware.of(player, pump)) {
            if (pump.afterDamage(ctx)) {
                CyberData.sync(player);
                return;
            }
        }
    }
}
