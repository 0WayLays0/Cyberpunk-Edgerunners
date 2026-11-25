package com.waylays.chromeneon.common.cyberware;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Cyberware events, posted on the NeoForge event bus on the server. Other systems (and other mods) react to
 * implants through them instead of reaching into the implants.
 */
public abstract class CyberwareEvent extends PlayerEvent {
    private final ResourceLocation implant;
    private final ResourceLocation slot;

    protected CyberwareEvent(ServerPlayer player, ResourceLocation implant, ResourceLocation slot) {
        super(player);
        this.implant = implant;
        this.slot = slot;
    }

    public ResourceLocation implant() {
        return implant;
    }

    public ResourceLocation slot() {
        return slot;
    }

    /** An implant was installed at a ripperdoc chair (or by command). */
    public static final class Installed extends CyberwareEvent {
        public Installed(ServerPlayer player, ResourceLocation implant, ResourceLocation slot) {
            super(player, implant, slot);
        }
    }

    /** An implant was taken out. */
    public static final class Removed extends CyberwareEvent {
        public Removed(ServerPlayer player, ResourceLocation implant, ResourceLocation slot) {
            super(player, implant, slot);
        }
    }

    /** An implant acted (used by the player, or firing by itself). */
    public static final class Used extends CyberwareEvent {
        public Used(ServerPlayer player, ResourceLocation implant, ResourceLocation slot) {
            super(player, implant, slot);
        }
    }

    /** An implant broke down for a while (humanity too low). */
    public static final class Malfunction extends CyberwareEvent {
        private final int ticks;

        public Malfunction(ServerPlayer player, ResourceLocation implant, ResourceLocation slot, int ticks) {
            super(player, implant, slot);
            this.ticks = ticks;
        }

        public int ticks() {
            return ticks;
        }
    }
}
