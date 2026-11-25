package com.waylays.chromeneon.common.cyberware;

import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.player.PlayerCyberData;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The server-side view of a player's cyberware for the rest of the mod: which implants they carry, where, in what
 * state. Systems that interact with implants (combat, movement, later hacking and VESPER) ask here instead of
 * reading the data themselves.
 */
public final class Cyberware {
    private Cyberware() {}

    /** Every installed implant with its context, now. */
    public static List<ImplantContext> all(ServerPlayer player) {
        PlayerCyberData data = CyberData.get(player);
        long now = player.level().getGameTime();
        List<ImplantContext> out = new ArrayList<>();
        for (CyberwareLoadout.Entry e : data.loadout().entries()) {
            out.add(new ImplantContext(player, data, e.slot(), e.index(), e.installed(), now));
        }
        return out;
    }

    /** Every installed copy of {@code implant} (blades can be in both arms). */
    public static List<ImplantContext> of(ServerPlayer player, Implant implant) {
        ResourceLocation id = ModImplants.id(implant);
        return all(player).stream().filter(c -> c.installed().id().equals(id)).toList();
    }

    public static Optional<ImplantContext> first(ServerPlayer player, Implant implant) {
        return of(player, implant).stream().findFirst();
    }

    /** Installed, switched on and not malfunctioning. */
    public static boolean working(ServerPlayer player, Implant implant) {
        return of(player, implant).stream().anyMatch(ImplantContext::working);
    }

    /** An installed implant of a given class, with its context. */
    public record Fitted<T extends Implant>(T implant, ImplantContext ctx) {}

    /** Every installed implant of a class: each model of the accelerator, for instance. */
    public static <T extends Implant> List<Fitted<T>> ofType(ServerPlayer player, Class<T> type) {
        List<Fitted<T>> out = new ArrayList<>();
        for (ImplantContext ctx : all(player)) {
            implant(ctx).filter(type::isInstance).ifPresent(i -> out.add(new Fitted<>(type.cast(i), ctx)));
        }
        return out;
    }

    public static Optional<Implant> implant(ImplantContext ctx) {
        return ModImplants.get(ctx.installed().id());
    }
}
