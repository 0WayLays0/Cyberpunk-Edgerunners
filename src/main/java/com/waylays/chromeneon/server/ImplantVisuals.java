package com.waylays.chromeneon.server;

import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.mechanics.TimeDilation;
import com.waylays.chromeneon.common.network.ImplantVisualsPayload;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.server.level.ServerPlayer;
import com.waylays.chromeneon.common.network.ChromeNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tells the players who can see someone what of their cyberware shows, when it changes. */
public final class ImplantVisuals {
    private static final Map<UUID, ImplantVisualsPayload> LAST = new HashMap<>();

    private ImplantVisuals() {}

    public static int flags(ServerPlayer player) {
        int flags = 0;
        for (var ctx : Cyberware.of(player, ModImplants.REAPER_BLADES.get())) {
            if (!ctx.working() || !ctx.state().active) continue;
            flags |= ctx.slot().equals(ModBodySlots.LEFT_ARM) ? ImplantVisualsPayload.LEFT_BLADE : ImplantVisualsPayload.RIGHT_BLADE;
        }
        return flags;
    }

    public static ImplantVisualsPayload of(ServerPlayer player) {
        return new ImplantVisualsPayload(player.getId(), flags(player), TimeDilation.slowdown(player));
    }

    /**
     * Sends a change to the player and to everyone close enough to see them (the server's view distance): a client
     * that does not track the player yet only keeps the value until it does.
     */
    public static void update(ServerPlayer player) {
        ImplantVisualsPayload now = of(player);
        ImplantVisualsPayload last = LAST.put(player.getUUID(), now);
        if (now.equals(last)) return;
        double range = player.server.getPlayerList().getViewDistance() * 16.0;
        for (ServerPlayer viewer : player.serverLevel().players()) {
            if (viewer == player || viewer.distanceToSqr(player) <= range * range) ChromeNetwork.send(viewer, now);
        }
    }

    /** A player starts seeing {@code target}: send its current look. */
    public static void showTo(ServerPlayer viewer, ServerPlayer target) {
        ChromeNetwork.send(viewer, of(target));
    }

    static void forget(ServerPlayer player) {
        LAST.remove(player.getUUID());
    }
}
