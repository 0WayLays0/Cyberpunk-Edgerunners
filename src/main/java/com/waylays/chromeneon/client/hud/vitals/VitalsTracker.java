package com.waylays.chromeneon.client.hud.vitals;

import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudTime;
import com.waylays.chromeneon.client.hud.HudVisibility;
import com.waylays.chromeneon.client.hud.anim.Smoothed;
import com.waylays.chromeneon.common.hud.NotificationKind;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/**
 * Watches the player's health and armor tick by tick and turns their changes into events for the HUD: damage
 * (with its direction), healing, the critical state (with hysteresis so it does not flicker at the threshold) and
 * armor gained or lost. Everything health-related on screen reads from here.
 */
public final class VitalsTracker {
    static final float CRITICAL_ENTER = 0.30F, CRITICAL_EXIT = 0.36F;

    private LocalPlayer tracked;
    float health, maxHealth = 20F, absorption;
    /** The health before recent damage: drawn as a fading chunk behind the bar. */
    float trail;
    private int trailHold;
    float healFrom, healTo, healAt = -1000F, damageAt = -1000F;
    boolean critical;
    float criticalAt = -1000F;
    int armor, armorFrom;
    float armorAt = -1000F, toughness;
    final Smoothed shown = new Smoothed();
    private final Smoothed alert = new Smoothed();

    public void tick(LocalPlayer player) {
        if (player == null) {
            tracked = null;
            return;
        }
        if (player != tracked) {
            reset(player);
            return;
        }
        float now = HudTime.ticks();
        float h = player.getHealth(), m = Math.max(1F, player.getMaxHealth());
        if (h < health - 0.01F) onDamage(player, health - h, m, now);
        else if (h > health + 0.01F) {
            healFrom = health;
            healTo = h;
            healAt = now;
        }
        health = h;
        maxHealth = m;
        absorption = player.getAbsorptionAmount();
        if (trailHold > 0) trailHold--;
        else trail = Math.max(health, trail - Math.max(0.25F, (trail - health) * 0.18F));

        float ratio = health / maxHealth;
        if (!critical && health > 0F && ratio <= CRITICAL_ENTER && HudVisibility.vulnerable()) {
            critical = true;
            criticalAt = now;
            ChromeHud.notify(NotificationKind.ALERT, Component.translatable("chromeneon.hud.critical.title"),
                    Component.translatable("chromeneon.hud.critical.message"));
            ChromeHud.glitch(0.7F, 12);
        } else if (critical && (ratio > CRITICAL_EXIT || health <= 0F)) {
            critical = false;
        }

        int a = player.getArmorValue();
        if (a != armor) {
            armorFrom = armor;
            armor = a;
            armorAt = now;
        }
        toughness = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
    }

    private void reset(LocalPlayer player) {
        tracked = player;
        health = trail = player.getHealth();
        maxHealth = Math.max(1F, player.getMaxHealth());
        absorption = player.getAbsorptionAmount();
        armor = armorFrom = player.getArmorValue();
        critical = false;
        shown.snap(health);
        ChromeHud.damage().clear();
    }

    private void onDamage(LocalPlayer player, float amount, float max, float now) {
        trail = Math.max(trail, health);
        trailHold = 10;
        damageAt = now;
        float share = amount / max;
        ChromeHud.damage().hit(share, sourceYaw(player));
        if (share >= 0.25F) ChromeHud.glitch(0.35F + share, 6 + Math.round(share * 12));
    }

    /** World yaw from the player toward what hurt them, NaN for damage without a position (fall, hunger...). */
    private static float sourceYaw(LocalPlayer player) {
        DamageSource source = player.getLastDamageSource();
        Vec3 pos = source == null ? null : source.getSourcePosition();
        if (pos == null) return Float.NaN;
        double dx = pos.x - player.getX(), dz = pos.z - player.getZ();
        if (dx * dx + dz * dz < 0.01) return Float.NaN;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public boolean critical() {
        return critical;
    }

    /** 0..1: how deep into the critical zone the player is, for the heartbeat. */
    public float criticalSeverity() {
        if (!critical || !HudVisibility.vulnerable()) return 0F;
        return Math.clamp(1F - (health / maxHealth) / CRITICAL_ENTER, 0F, 1F) * 0.6F + 0.4F;
    }

    /** Danger level the whole interface leans toward, smoothed. */
    public float alert(float delta) {
        float target = 0F;
        if (tracked != null && health > 0F && HudVisibility.vulnerable()) {
            float ratio = health / maxHealth;
            if (critical) target = 0.6F + 0.4F * Math.clamp(1F - ratio / CRITICAL_ENTER, 0F, 1F);
            else if (ratio < 0.5F) target = (0.5F - ratio) / 0.5F * 0.35F;
        }
        return alert.update(target, delta, 0.15F);
    }
}
