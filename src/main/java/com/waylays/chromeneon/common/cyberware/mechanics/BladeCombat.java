package com.waylays.chromeneon.common.cyberware.mechanics;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.UseResult;
import com.waylays.chromeneon.common.cyberware.implants.ArgusOptics;
import com.waylays.chromeneon.common.player.PlayerCyberData;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The blades in combat. Deployed with an empty main hand, they are the player's weapon: the attack's damage and
 * speed come from them (attribute modifiers that exist only in that state, so vanilla's attack, critical hits,
 * cooldown and enchantment logic all apply). The lunge dashes forward, toward the optics' target when there is one,
 * and cuts every creature crossed once.
 */
public final class BladeCombat {
    private static final ResourceLocation DAMAGE_ID = ChromeNeon.id("blades_damage");
    private static final ResourceLocation SPEED_ID = ChromeNeon.id("blades_speed");
    public static final int LUNGE_TICKS = 6;
    private static final double TARGET_RANGE = 12.0;

    private BladeCombat() {}

    /** Deployed and working blade sets. */
    public static List<ImplantContext> deployed(ServerPlayer player) {
        return Cyberware.of(player, ModImplants.REAPER_BLADES.get()).stream().filter(c -> c.working() && c.state().active).toList();
    }

    /** Whether the blades are the player's weapon right now. */
    public static boolean armed(ServerPlayer player) {
        return player.getMainHandItem().isEmpty() && !deployed(player).isEmpty();
    }

    public static void tick(ServerPlayer player, PlayerCyberData data) {
        int blades = player.getMainHandItem().isEmpty() ? deployed(player).size() : 0;
        modifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, blades * ServerConfig.BLADE_DAMAGE.get());
        modifier(player.getAttribute(Attributes.ATTACK_SPEED), SPEED_ID, blades > 0 ? ServerConfig.BLADE_SPEED.get() : 0.0);
        if (data.lungeTicks > 0) {
            data.lungeTicks--;
            cut(player, data);
        }
    }

    private static void modifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(id);
        if (amount <= 0.0) {
            if (current != null) attribute.removeModifier(id);
        } else if (current == null || current.amount() != amount) {
            attribute.removeModifier(id);
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** Extra share of damage while time is dilated (the accelerator feeds the blades). */
    public static float damageFactor(ServerPlayer player) {
        return TimeDilation.isDilating(player) ? 1F + ServerConfig.DILATION_BLADE_BONUS.get().floatValue() : 1F;
    }

    /** The player asked to lunge; {@code targetId} is the entity their optics show, or -1. */
    public static UseResult lunge(ServerPlayer player, PlayerCyberData data, int targetId) {
        List<ImplantContext> blades = deployed(player);
        if (blades.isEmpty()) return Cyberware.of(player, ModImplants.REAPER_BLADES.get()).isEmpty() ? UseResult.NOT_INSTALLED : UseResult.OFFLINE;
        if (!player.getMainHandItem().isEmpty() || player.isPassenger() || player.isFallFlying() || !player.isAlive()) return UseResult.NOT_NOW;
        ImplantContext first = blades.getFirst();
        long now = first.now();
        if (first.state().coolingDown(now)) return UseResult.COOLDOWN;
        Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
        Entity target = targetId >= 0 ? player.level().getEntity(targetId) : null;
        boolean guided = target instanceof LivingEntity && target.isAlive() && target.distanceTo(player) <= TARGET_RANGE
                && player.hasLineOfSight(target) && Cyberware.of(player, ModImplants.ARGUS_OPTICS.get()).stream().anyMatch(ArgusOptics::online);
        if (guided) dir = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
        if (dir.lengthSqr() < 1e-4) return UseResult.NOT_NOW;
        int cooldown = ServerConfig.LUNGE_COOLDOWN.get();
        if (TimeDilation.isDilating(player)) cooldown /= 2;
        long ready = now + cooldown;
        blades.forEach(b -> b.state().cooldownUntil = ready);
        double speed = ServerConfig.LUNGE_SPEED.get();
        player.setDeltaMovement(dir.x * speed, 0.12, dir.z * speed);
        player.hurtMarked = true;
        data.lungeTicks = LUNGE_TICKS;
        data.lungeHits.clear();
        player.level().playSound(null, player.blockPosition(), ModSounds.BLADES_LUNGE.get(), SoundSource.PLAYERS, 1F, 1F);
        return UseResult.OK;
    }

    /** During a lunge: cuts each creature the player passes through, once. */
    private static void cut(ServerPlayer player, PlayerCyberData data) {
        float damage = ServerConfig.LUNGE_DAMAGE.get().floatValue() * damageFactor(player);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.0),
                e -> e != player && e.isAlive() && !e.isAlliedTo(player))) {
            if (!data.lungeHits.add(e.getId())) continue;
            if (e.hurt(player.damageSources().playerAttack(player), damage)) {
                Vec3 push = e.position().subtract(player.position()).multiply(1, 0, 1).normalize();
                e.knockback(0.6, -push.x, -push.z);
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.6), e.getZ(), 1, 0, 0, 0, 0);
                    level.playSound(null, e.blockPosition(), ModSounds.BLADES_HIT.get(), SoundSource.PLAYERS, 1F, 1F);
                }
            }
        }
    }
}
