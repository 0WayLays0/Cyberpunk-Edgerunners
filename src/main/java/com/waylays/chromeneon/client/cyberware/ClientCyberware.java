package com.waylays.chromeneon.client.cyberware;

import com.waylays.chromeneon.common.cyberware.CyberwareLoadout;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantState;
import com.waylays.chromeneon.common.network.ImplantVisualsPayload;
import com.waylays.chromeneon.common.network.SyncCyberStatePayload;
import com.waylays.chromeneon.common.player.SyncedCyberState;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What this client knows of cyberware: its own implants and their states (from the server's last sync) and what
 * shows of other players' (blades out, time dilated). Read-only: every change goes through a request.
 */
public final class ClientCyberware {
    private static final Map<Integer, Integer> VISUALS = new HashMap<>();

    private ClientCyberware() {}

    /** Server time, as the client's level tracks it (implant times are server times). */
    public static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    public static List<CyberwareLoadout.Entry> entries() {
        SyncCyberStatePayload s = SyncedCyberState.current();
        return s == null ? List.of() : s.loadout().entries();
    }

    public static List<CyberwareLoadout.Entry> of(Implant implant) {
        ResourceLocation id = ModImplants.id(implant);
        return entries().stream().filter(e -> e.installed().id().equals(id)).toList();
    }

    public static Optional<CyberwareLoadout.Entry> first(Implant implant) {
        return of(implant).stream().findFirst();
    }

    public static boolean working(CyberwareLoadout.Entry entry) {
        return entry.installed().state().working(now());
    }

    public static Optional<ImplantState> state(Implant implant) {
        return first(implant).map(e -> e.installed().state());
    }

    /** Installed, working and switched on (blades out, optics online). */
    public static boolean activeAndWorking(Implant implant) {
        return of(implant).stream().anyMatch(e -> working(e) && e.installed().state().active);
    }

    public static boolean opticsOnline() {
        return activeAndWorking(ModImplants.ARGUS_OPTICS.get());
    }

    /** Whether this player's accelerator is running (the client slows what is around it too). */
    public static boolean dilating() {
        return of(ModImplants.CHRONOS_ACCELERATOR.get()).stream()
                .anyMatch(e -> e.installed().state().active && now() < e.installed().state().activeUntil);
    }

    /** Whether a tactical analysis runs: paid for, or free while time is dilated. */
    public static boolean analysing() {
        if (!opticsOnline()) return false;
        if (dilating()) return true;
        return state(ModImplants.ARGUS_OPTICS.get()).map(s -> now() < s.activeUntil).orElse(false);
    }

    // ---------------------------------------------------------------- other players

    public static void acceptVisuals(ImplantVisualsPayload payload) {
        VISUALS.put(payload.entityId(), payload.flags());
    }

    public static int visuals(int entityId) {
        return VISUALS.getOrDefault(entityId, 0);
    }

    public static void clear() {
        VISUALS.clear();
    }
}
