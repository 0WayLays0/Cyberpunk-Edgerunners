package com.waylays.chromeneon.common.cyberware.mechanics;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.CyberwareEvent;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.hud.HudMessages;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.player.CyberStats;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.NeoForge;

/**
 * What humanity does. Above the strain threshold, nothing. Below it, every working implant has a chance each second
 * to break down for a while (it stops, then comes back by itself), rising as humanity falls. It is the price of the
 * implants the player chose to carry, never permanent, and removing implants restores humanity at once.
 */
public final class Malfunctions {
    public static final int CHECKS_PER_MINUTE = 60;

    private Malfunctions() {}

    /** Called once per second per player. */
    public static void check(ServerPlayer player, CyberStats stats) {
        if (!ServerConfig.MALFUNCTIONS.get()) return;
        double chance = CyberMath.malfunctionChance(stats.humanityRatio(), ServerConfig.STRAIN_THRESHOLD.get(),
                ServerConfig.MALFUNCTION_CHANCE.get(), CHECKS_PER_MINUTE);
        if (chance <= 0) return;
        for (ImplantContext ctx : Cyberware.all(player)) {
            if (ctx.working() && player.getRandom().nextDouble() < chance) trigger(ctx, ServerConfig.MALFUNCTION_TICKS.get());
        }
    }

    /** Breaks an implant down for {@code ticks}. */
    public static void trigger(ImplantContext ctx, int ticks) {
        ctx.state().malfunctionUntil = ctx.now() + ticks;
        Cyberware.implant(ctx).ifPresent(i -> i.onStopped(ctx));
        ServerPlayer player = ctx.player();
        player.level().playSound(null, player.blockPosition(), ModSounds.MALFUNCTION.get(), SoundSource.PLAYERS, 0.9F, 1F);
        Component name = Component.translatable(implantKey(ctx));
        HudMessages.notify(player, NotificationKind.ERROR, Component.translatable("chromeneon.notify.malfunction.title"),
                Component.translatable("chromeneon.notify.malfunction.message", name, ticks / 20));
        NeoForge.EVENT_BUS.post(new CyberwareEvent.Malfunction(player, ctx.installed().id(), ctx.slot(), ticks));
    }

    private static String implantKey(ImplantContext ctx) {
        return "chromeneon.implant." + ctx.installed().id().getPath();
    }

    /** For other systems: the description key of an implant. */
    public static String key(Implant implant) {
        return "chromeneon.implant." + com.waylays.chromeneon.common.registry.ModImplants.id(implant).getPath();
    }
}
