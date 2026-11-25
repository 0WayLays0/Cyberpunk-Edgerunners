package com.waylays.chromeneon.client.hud;

import com.waylays.chromeneon.client.hud.fx.BootSequence;
import com.waylays.chromeneon.client.hud.fx.DamageFx;
import com.waylays.chromeneon.client.hud.fx.GlitchController;
import com.waylays.chromeneon.client.hud.nav.MapMarkers;
import com.waylays.chromeneon.client.hud.nav.MapPings;
import com.waylays.chromeneon.client.hud.nav.TerrainSampler;
import com.waylays.chromeneon.client.hud.notify.CallCenter;
import com.waylays.chromeneon.client.hud.notify.IncomingCall;
import com.waylays.chromeneon.client.hud.notify.Notification;
import com.waylays.chromeneon.client.hud.notify.NotificationQueue;
import com.waylays.chromeneon.client.hud.scan.ScanRegistry;
import com.waylays.chromeneon.client.hud.scan.TargetTracker;
import com.waylays.chromeneon.client.hud.scan.WorldProjection;
import com.waylays.chromeneon.client.hud.vitals.RamTracker;
import com.waylays.chromeneon.client.hud.vitals.VitalsTracker;
import com.waylays.chromeneon.common.hud.NotificationKind;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * The HUD's public face on the client. Every module of the mod talks to the HUD through these methods —
 * notifications, glitches, calls, RAM feedback, map pings, scan providers — and never draws on the screen itself.
 * (Server-side code uses {@code common.hud.HudMessages}, which arrives here.)
 */
public final class ChromeHud {
    private static final VitalsTracker VITALS = new VitalsTracker();
    private static final RamTracker RAM = new RamTracker();
    private static final GlitchController GLITCH = new GlitchController();
    private static final DamageFx DAMAGE = new DamageFx();
    private static final BootSequence BOOT = new BootSequence();
    private static final NotificationQueue NOTIFICATIONS = new NotificationQueue();
    private static final CallCenter CALLS = new CallCenter();
    private static final TargetTracker TARGET = new TargetTracker();
    private static final ScanRegistry SCANNERS = new ScanRegistry();
    private static final WorldProjection PROJECTION = new WorldProjection();
    private static final TerrainSampler TERRAIN = new TerrainSampler();
    private static final MapMarkers MARKERS = new MapMarkers();
    private static final MapPings PINGS = new MapPings();

    private ChromeHud() {}

    // ---------------------------------------------------------------- API for the mod's systems

    public static void notify(Notification notification) {
        NOTIFICATIONS.push(notification, HudTime.ticks());
        if (notification.kind() == NotificationKind.ALERT || notification.kind() == NotificationKind.ERROR) glitch(0.35F, 6);
    }

    public static void notify(NotificationKind kind, Component title, Component message) {
        notify(Notification.builder(kind, title).message(message).build());
    }

    /** A controlled glitch: intensity 0..1, at most {@link GlitchController#MAX_TICKS} ticks. */
    public static void glitch(float intensity, int ticks) {
        GLITCH.trigger(intensity, ticks);
    }

    public static void incomingCall(IncomingCall call) {
        CALLS.ring(call);
    }

    /** An action needed {@code cost} RAM cells and could not run. */
    public static void ramInsufficient(int cost) {
        RAM.insufficient(cost);
    }

    /** An expanding ring on the minimap at a world position ({@code radius} in blocks). */
    public static void ping(Vec3 pos, float radius, int color) {
        PINGS.add(pos, radius, color);
    }

    public static ScanRegistry scanners() {
        return SCANNERS;
    }

    // ---------------------------------------------------------------- internals shared by the HUD's parts

    public static VitalsTracker vitals() {
        return VITALS;
    }

    public static RamTracker ram() {
        return RAM;
    }

    public static GlitchController glitch() {
        return GLITCH;
    }

    public static DamageFx damage() {
        return DAMAGE;
    }

    public static BootSequence boot() {
        return BOOT;
    }

    public static NotificationQueue notifications() {
        return NOTIFICATIONS;
    }

    public static CallCenter calls() {
        return CALLS;
    }

    public static TargetTracker target() {
        return TARGET;
    }

    public static WorldProjection projection() {
        return PROJECTION;
    }

    public static TerrainSampler terrain() {
        return TERRAIN;
    }

    public static MapMarkers markers() {
        return MARKERS;
    }

    public static MapPings pings() {
        return PINGS;
    }

    /** Leaving a world: nothing from it may show in the next one. */
    public static void reset() {
        NOTIFICATIONS.clear();
        CALLS.clear();
        TARGET.clear();
        TERRAIN.clear();
        PINGS.clear();
        DAMAGE.clear();
        MARKERS.unload();
    }
}
