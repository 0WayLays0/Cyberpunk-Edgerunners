package com.waylays.chromeneon.client.hud.scan.providers;

import com.waylays.chromeneon.client.hud.scan.ScanClass;
import com.waylays.chromeneon.client.hud.scan.ScanInfo;
import com.waylays.chromeneon.client.hud.scan.ScanProvider;
import com.waylays.chromeneon.client.hud.scan.ScanSubject;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Creatures and players, from what the client is sent about them: health, armor, whether a mob is aggressive.
 * Threat comes from the creature's nature (hostile, boss), not from guessed numbers.
 */
public final class LivingScanProvider implements ScanProvider {
    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean supports(ScanSubject subject, ClientLevel level) {
        return subject instanceof ScanSubject.EntitySubject e && e.entity() instanceof LivingEntity;
    }

    @Override
    public ScanInfo describe(ScanSubject subject, ClientLevel level, Player viewer) {
        LivingEntity e = (LivingEntity) ((ScanSubject.EntitySubject) subject).entity();
        ScanClass kind = e instanceof Player ? ScanClass.PLAYER
                : e instanceof Enemy ? ScanClass.HOSTILE
                : e instanceof NeutralMob ? ScanClass.NEUTRAL : ScanClass.PASSIVE;
        boolean boss = e instanceof WitherBoss || e instanceof EnderDragon;
        int threat = boss ? 3 : switch (kind) {
            case HOSTILE -> 2;
            case NEUTRAL, PLAYER -> 1;
            default -> 0;
        };
        List<ScanInfo.Line> lines = new ArrayList<>();
        lines.add(Lines.line("type", e.getType().getDescription()));
        lines.add(Lines.line("health", String.format(java.util.Locale.ROOT, "%.0f / %.0f", e.getHealth(), e.getMaxHealth())));
        if (e.getArmorValue() > 0) lines.add(Lines.line("armor", String.valueOf(e.getArmorValue())));
        if (e instanceof Mob mob) {
            boolean aggressive = mob.isAggressive();
            lines.add(Lines.line("behaviour", Component.translatable(aggressive
                    ? "chromeneon.scan.behaviour.aggressive" : "chromeneon.scan.behaviour.calm"),
                    aggressive ? NeonPalette.ORANGE : NeonPalette.CYAN_SOFT));
        }
        if (e instanceof Player) {
            lines.add(Lines.line("cyberware", Component.translatable("chromeneon.scan.unknown"), NeonPalette.LAVENDER));
        }
        lines.add(Lines.line("threat", Component.translatable("chromeneon.scan.threat." + threat),
                threat >= 2 ? NeonPalette.ORANGE : NeonPalette.CYAN_SOFT));
        int ticks = kind == ScanClass.PLAYER ? 40 : kind == ScanClass.HOSTILE ? 30 : 20;
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        return new ScanInfo(e.getDisplayName(), kind, Lines.designation(e.getUUID(), path), threat,
                e.getHealth() / Math.max(1F, e.getMaxHealth()), lines, ticks);
    }
}
