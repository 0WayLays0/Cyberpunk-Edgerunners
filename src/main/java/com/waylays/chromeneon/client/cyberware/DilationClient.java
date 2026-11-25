package com.waylays.chromeneon.client.cyberware;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.mechanics.CyberMath;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The accelerator as this player sees it: while their own time dilation runs, the client slows the same entities
 * the server slows (same zone, same rhythm), so animations match the slowed movement instead of running at full
 * speed on the spot.
 */
@EventBusSubscriber(modid = ChromeNeon.MODID, value = Dist.CLIENT)
public final class DilationClient {
    private DilationClient() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() || entity instanceof Player) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !ClientCyberware.dilating()) return;
        if (entity instanceof Projectile p && p.getOwner() == player) return;
        double r = ServerConfig.ACCEL_RADIUS.get();
        if (entity.distanceToSqr(player) > r * r) return;
        if (!CyberMath.livesThisTick(entity.level().getGameTime(), entity.getId(), ServerConfig.ACCEL_SLOWDOWN.get())) {
            event.setCanceled(true);
        }
    }
}
