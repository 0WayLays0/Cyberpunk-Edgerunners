package com.waylays.chromeneon.common.cyberware.mechanics;

import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The time accelerator's effect, done for real: around a player with the accelerator running, every other entity
 * (creatures, projectiles, items, vehicles) only lives one tick out of N, so it moves, attacks, falls and flies N
 * times slower, while the player acts at full speed. Players are never slowed (the server cannot slow a client);
 * the player's own projectiles keep their speed. Blocks, fluids and redstone are not slowed.
 */
@EventBusSubscriber(modid = ChromeNeon.MODID)
public final class TimeDilation {
    /** A slowed zone following its owner. */
    public record Field(UUID owner, ResourceKey<Level> dimension, Vec3 center, double radius, int slowdown, long until) {}

    private static final Map<UUID, Field> FIELDS = new HashMap<>();

    private TimeDilation() {}

    public static void start(ServerPlayer player, double radius, int slowdown, long until) {
        FIELDS.put(player.getUUID(), new Field(player.getUUID(), player.level().dimension(), player.position(), radius, slowdown, until));
    }

    public static void stop(ServerPlayer player) {
        FIELDS.remove(player.getUUID());
    }

    public static boolean isDilating(Player player) {
        return slowdown(player) > 1;
    }

    /** The slowdown of the player's running zone, or 0 when none runs. */
    public static int slowdown(Player player) {
        Field f = FIELDS.get(player.getUUID());
        return f != null && player.level().getGameTime() < f.until() ? f.slowdown() : 0;
    }

    /** Moves each zone with its owner; drops the zones of players gone or expired. */
    public static void follow(ServerPlayer player) {
        Field f = FIELDS.get(player.getUUID());
        if (f == null) return;
        if (!player.isAlive() || player.level().getGameTime() >= f.until()) {
            FIELDS.remove(player.getUUID());
            return;
        }
        FIELDS.put(player.getUUID(), new Field(f.owner(), player.level().dimension(), player.position(), f.radius(), f.slowdown(), f.until()));
    }

    /** Whether {@code entity} is inside a zone and must skip this tick. */
    public static boolean skips(Entity entity, long gameTime) {
        if (FIELDS.isEmpty() || entity instanceof Player) return false;
        for (Field f : FIELDS.values()) {
            if (f.dimension() != entity.level().dimension() || gameTime >= f.until()) continue;
            if (entity instanceof Projectile p && p.getOwner() != null && p.getOwner().getUUID().equals(f.owner())) continue;
            if (entity.position().distanceToSqr(f.center()) > f.radius() * f.radius()) continue;
            return !CyberMath.livesThisTick(gameTime, entity.getId(), f.slowdown());
        }
        return false;
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() && skips(entity, entity.level().getGameTime())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        FIELDS.clear();
    }
}
