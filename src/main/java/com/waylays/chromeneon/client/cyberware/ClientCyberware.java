package com.waylays.chromeneon.client.cyberware;

import com.waylays.chromeneon.common.cyberware.CyberwareLoadout;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantState;
import com.waylays.chromeneon.common.cyberware.implants.ChronosAccelerator;
import com.waylays.chromeneon.common.network.ImplantVisualsPayload;
import com.waylays.chromeneon.common.network.SyncCyberStatePayload;
import com.waylays.chromeneon.common.player.SyncedCyberState;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What this client knows of cyberware: its own implants and their states (from the server's last sync) and what
 * shows of every player's (blades out, time dilated). Read-only: every change goes through a request.
 */
public final class ClientCyberware {
    private static final Map<Integer, ImplantVisualsPayload> VISUALS = new HashMap<>();
    private static final Map<Integer, Integer> DILATIONS = new HashMap<>();
    private static final Map<Integer, Integer> DILATIONS_VIEW = java.util.Collections.unmodifiableMap(DILATIONS);

    /** One of this player's installed implants with its entry. */
    public record Fitted<T extends Implant>(T implant, CyberwareLoadout.Entry entry) {
        public ImplantState state() {
            return entry.installed().state();
        }
    }

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

    /** This player's installed implants of a class (every accelerator model). */
    public static <T extends Implant> List<Fitted<T>> ofType(Class<T> type) {
        List<Fitted<T>> out = new ArrayList<>();
        for (CyberwareLoadout.Entry e : entries()) {
            ModImplants.get(e.installed().id()).filter(type::isInstance).ifPresent(i -> out.add(new Fitted<>(type.cast(i), e)));
        }
        return out;
    }

    /** This player's operating system, if it is an accelerator. */
    public static Optional<Fitted<ChronosAccelerator>> accelerator() {
        return ofType(ChronosAccelerator.class).stream().findFirst();
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

    /** Whether this player's accelerator is running. */
    public static boolean dilating() {
        return accelerator().map(f -> f.state().active && now() < f.state().activeUntil).orElse(false);
    }

    /** Whether a tactical analysis runs: paid for, or free while time is dilated. */
    public static boolean analysing() {
        if (!opticsOnline()) return false;
        if (dilating()) return true;
        return state(ModImplants.ARGUS_OPTICS.get()).map(s -> now() < s.activeUntil).orElse(false);
    }

    // ---------------------------------------------------------------- other players

    public static void acceptVisuals(ImplantVisualsPayload payload) {
        if (payload.flags() == 0 && payload.slowdown() == 0) VISUALS.remove(payload.entityId());
        else VISUALS.put(payload.entityId(), payload);
        if (payload.slowdown() > 1) DILATIONS.put(payload.entityId(), payload.slowdown());
        else DILATIONS.remove(payload.entityId());
    }

    /** Blades shown on a player ({@link ImplantVisualsPayload} flags). */
    public static int visuals(int entityId) {
        ImplantVisualsPayload v = VISUALS.get(entityId);
        return v == null ? 0 : v.flags();
    }

    /** Slowdown of the time dilation a player runs, 0 when none. */
    public static int slowdown(int entityId) {
        ImplantVisualsPayload v = VISUALS.get(entityId);
        return v == null ? 0 : v.slowdown();
    }

    /** Every player known to run a time dilation, by entity id, with its slowdown (read-only view). */
    public static Map<Integer, Integer> dilations() {
        return DILATIONS_VIEW;
    }

    public static void clear() {
        VISUALS.clear();
        DILATIONS.clear();
    }
}
