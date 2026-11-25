package com.waylays.chromeneon.client.hud.scan;

import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudTime;
import com.waylays.chromeneon.common.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Finds the real target in the player's line of sight each tick (a creature up to the targeting range, blocks
 * stopping the ray; while scanning, also the block looked at) and runs the scan on it: holding the scan key fills
 * the scan, which completes after the target's scan time and is remembered for a minute. A target lost for a few
 * ticks is kept, so brackets do not flicker.
 */
public final class TargetTracker {
    private static final int GRACE = 6, MEMORY = 1200;

    private ScanSubject subject;
    private ScanInfo info;
    private float acquiredAt, completedAt = Float.NaN, scanPressedAt = -1000F;
    private float progress;
    private int lost;
    private boolean scanning;
    private final Map<Object, Long> remembered = new HashMap<>();

    public void tick(Minecraft mc, boolean scanKey) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            clear();
            return;
        }
        float now = HudTime.ticks();
        boolean was = scanning;
        scanning = scanKey && mc.screen == null && !player.isSpectator();
        if (scanning && !was) scanPressedAt = now;

        ScanSubject found = find(mc, player);
        if (found != null && subject != null && found.key().equals(subject.key())) {
            subject = found;
            lost = 0;
        } else if (found != null) {
            acquire(found, now);
        } else if (subject != null && ++lost > GRACE) {
            subject = null;
        }
        if (subject instanceof ScanSubject.EntitySubject e && !e.entity().isAlive()) subject = null;
        if (subject == null) {
            info = null;
            return;
        }
        info = ChromeHud.scanners().describe(subject, mc.level, player);
        if (Float.isNaN(completedAt)) {
            if (scanning) {
                if (++progress >= info.scanTicks()) complete(mc, now);
            } else {
                progress = Math.max(0F, progress - 2F);
            }
        }
        if (mc.level.getGameTime() % 200 == 0) remembered.values().removeIf(t -> t < mc.level.getGameTime());
    }

    private void acquire(ScanSubject found, float now) {
        subject = found;
        acquiredAt = now;
        lost = 0;
        progress = 0F;
        Long until = remembered.get(found.key());
        Minecraft mc = Minecraft.getInstance();
        completedAt = until != null && mc.level != null && until > mc.level.getGameTime() ? now - 100F : Float.NaN;
    }

    private void complete(Minecraft mc, float now) {
        completedAt = now;
        remembered.put(subject.key(), mc.level.getGameTime() + MEMORY);
        Vec3 at = subject instanceof ScanSubject.EntitySubject e ? e.entity().position()
                : Vec3.atCenterOf(((ScanSubject.BlockSubject) subject).pos());
        ChromeHud.ping(at, 6F, info.kind().color());
    }

    private ScanSubject find(Minecraft mc, LocalPlayer player) {
        double range = ClientConfig.TARGET_RANGE.get();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1F);
        HitResult blockHit = player.pick(range, 1F, false);
        double reach = blockHit.getType() == HitResult.Type.MISS ? range : blockHit.getLocation().distanceTo(eye);
        AABB area = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(look.scale(reach)), area,
                e -> !e.isSpectator() && e.isPickable() && !e.isInvisibleTo(player) && e != player.getVehicle(), reach * reach);
        if (entityHit != null) {
            Entity e = entityHit.getEntity();
            if (scanning || e instanceof LivingEntity) return new ScanSubject.EntitySubject(e);
        }
        if (scanning && blockHit instanceof BlockHitResult bh && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = mc.level.getBlockState(bh.getBlockPos());
            if (!state.isAir()) return new ScanSubject.BlockSubject(bh.getBlockPos().immutable(), state);
        }
        return null;
    }

    public void clear() {
        subject = null;
        info = null;
        scanning = false;
        remembered.clear();
    }

    public ScanSubject subject() {
        return subject;
    }

    public ScanInfo info() {
        return info;
    }

    public boolean scanning() {
        return scanning;
    }

    public float acquiredAt() {
        return acquiredAt;
    }

    /** NaN while the scan is not complete. */
    public float completedAt() {
        return completedAt;
    }

    public float scanPressedAt() {
        return scanPressedAt;
    }

    public float progress() {
        return info == null ? 0F : Math.clamp(progress / info.scanTicks(), 0F, 1F);
    }
}
