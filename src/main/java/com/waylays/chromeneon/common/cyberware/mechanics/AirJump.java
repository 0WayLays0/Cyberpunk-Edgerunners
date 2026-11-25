package com.waylays.chromeneon.common.cyberware.mechanics;

import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.UseResult;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.Optional;

/**
 * The tendons' extra jump, checked by the server. Movement is the client's in Minecraft, so the client pushes the
 * player up at once and reports it; the server checks the legs, the player's state and the jumps left, then counts
 * the jump, cancels the fall accumulated so far (so falling damage starts again from the jump) and shows it to the
 * other players. A refused report gets none of that.
 */
public final class AirJump {
    private AirJump() {}

    public static UseResult jump(ServerPlayer player) {
        Optional<ImplantContext> tendons = Cyberware.first(player, ModImplants.SPRING_TENDONS.get());
        if (tendons.isEmpty()) return UseResult.NOT_INSTALLED;
        ImplantContext ctx = tendons.get();
        if (!ctx.state().enabled) return UseResult.DISABLED;
        if (ctx.state().malfunctioning(ctx.now())) return UseResult.MALFUNCTION;
        boolean flying = player.getAbilities().flying || player.isFallFlying();
        if (!CyberMath.canAirJump(player.onGround(), flying, player.isInLiquid(), player.isPassenger(), ctx.state().charges)) {
            return ctx.state().charges <= 0 ? UseResult.NO_CHARGE : UseResult.NOT_NOW;
        }
        ctx.state().charges--;
        player.resetFallDistance();
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 8, 0.25, 0.02, 0.25, 0.02);
            level.playSound(null, player.blockPosition(), ModSounds.TENDONS_JUMP.get(), SoundSource.PLAYERS, 0.8F, 1F);
        }
        return UseResult.OK;
    }
}
