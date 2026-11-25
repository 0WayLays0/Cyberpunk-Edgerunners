package com.waylays.chromeneon.client.hud.nav;

import com.waylays.chromeneon.client.hud.HudTime;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Expanding rings on the minimap marking where something happened (a scan, later hacks and alarms). */
public final class MapPings {
    public static final float LIFE = 40F;

    public record Ping(Vec3 pos, float start, float radius, int color) {}

    private final List<Ping> pings = new ArrayList<>();

    public void add(Vec3 pos, float radius, int color) {
        pings.add(new Ping(pos, HudTime.ticks(), radius, color));
        if (pings.size() > 8) pings.removeFirst();
    }

    public List<Ping> active(float now) {
        pings.removeIf(p -> now - p.start() > LIFE);
        return pings;
    }

    public void clear() {
        pings.clear();
    }
}
