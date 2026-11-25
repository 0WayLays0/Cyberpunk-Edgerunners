package com.waylays.chromeneon.client.cyberware;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.mechanics.CyberMath;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;

/**
 * Time dilation as clients see it: around every player running an accelerator (this one included), the client
 * slows the same entities the server slows (same zone, same rhythm), so their animations match the slowed movement
 * instead of running at full speed on the spot.
 */
@EventBusSubscriber(modid = ChromeNeon.MODID, value = Dist.CLIENT)
public final class DilationClient {
    private DilationClient() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() || entity instanceof Player) return;
        Map<Integer, Integer> dilations = ClientCyberware.dilations();
        if (dilations.isEmpty()) return;
        double r = ServerConfig.ACCEL_RADIUS.get();
        for (var e : dilations.entrySet()) {
            Entity owner = entity.level().getEntity(e.getKey());
            if (owner == null || entity.distanceToSqr(owner) > r * r) continue;
            if (entity instanceof Projectile p && p.getOwner() == owner) continue;
            if (!CyberMath.livesThisTick(entity.level().getGameTime(), entity.getId(), e.getValue())) event.setCanceled(true);
            return;
        }
    }
}
