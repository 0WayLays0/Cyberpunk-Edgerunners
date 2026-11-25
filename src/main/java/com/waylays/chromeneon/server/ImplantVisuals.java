package com.waylays.chromeneon.server;

import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.mechanics.TimeDilation;
import com.waylays.chromeneon.common.network.ImplantVisualsPayload;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tells the players who can see someone what of their cyberware shows, when it changes. */
public final class ImplantVisuals {
    private static final Map<UUID, Integer> LAST = new HashMap<>();

    private ImplantVisuals() {}

    public static int flags(ServerPlayer player) {
        int flags = 0;
        for (var ctx : Cyberware.of(player, ModImplants.REAPER_BLADES.get())) {
            if (!ctx.working() || !ctx.state().active) continue;
            flags |= ctx.slot().equals(ModBodySlots.LEFT_ARM) ? ImplantVisualsPayload.LEFT_BLADE : ImplantVisualsPayload.RIGHT_BLADE;
        }
        if (TimeDilation.isDilating(player)) flags |= ImplantVisualsPayload.DILATING;
        return flags;
    }

    public static void update(ServerPlayer player) {
        int flags = flags(player);
        Integer last = LAST.put(player.getUUID(), flags);
        if (last == null || last != flags) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ImplantVisualsPayload(player.getId(), flags));
        }
    }

    /** A player starts seeing {@code target}: send its current look. */
    public static void showTo(ServerPlayer viewer, ServerPlayer target) {
        PacketDistributor.sendToPlayer(viewer, new ImplantVisualsPayload(target.getId(), flags(target)));
    }

    static void forget(ServerPlayer player) {
        LAST.remove(player.getUUID());
    }
}
