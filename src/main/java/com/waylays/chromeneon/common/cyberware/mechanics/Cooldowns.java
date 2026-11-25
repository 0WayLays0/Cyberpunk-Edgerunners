package com.waylays.chromeneon.common.cyberware.mechanics;

import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.CyberwareEvent;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import net.neoforged.neoforge.common.NeoForge;

/**
 * How long an implant cools down once the other implants and the listeners had their say: every working implant
 * applies its {@link Implant#cooldownFactor}, then a {@link CyberwareEvent.Cooldown} lets perks and other mods
 * change the result.
 */
public final class Cooldowns {
    private Cooldowns() {}

    public static int of(ImplantContext ctx, Implant implant, int base) {
        double factor = 1.0;
        for (ImplantContext other : Cyberware.all(ctx.player())) {
            if (!other.working()) continue;
            factor *= Cyberware.implant(other).map(i -> i.cooldownFactor(implant)).orElse(1.0);
        }
        var event = new CyberwareEvent.Cooldown(ctx.player(), ctx.installed().id(), ctx.slot(), base, CyberMath.cooldown(base, factor));
        NeoForge.EVENT_BUS.post(event);
        return event.ticks();
    }
}
