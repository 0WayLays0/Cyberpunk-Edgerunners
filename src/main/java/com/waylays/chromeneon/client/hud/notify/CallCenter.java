package com.waylays.chromeneon.client.hud.notify;

import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudTime;
import com.waylays.chromeneon.common.hud.NotificationKind;
import net.minecraft.network.chat.Component;

/** The one call ringing at a time, and how it ended (answered, refused or missed), for the popup's exit. */
public final class CallCenter {
    public enum Outcome { RINGING, ACCEPTED, DECLINED, MISSED }

    static final float OUTRO = 12F;
    private IncomingCall call;
    private float startedAt, endedAt;
    private Outcome outcome = Outcome.RINGING;

    /** A new call replaces one still ringing (which counts as missed). */
    public void ring(IncomingCall incoming) {
        if (call != null && outcome == Outcome.RINGING) end(Outcome.MISSED);
        call = incoming;
        startedAt = HudTime.ticks();
        outcome = Outcome.RINGING;
        ChromeHud.glitch(0.25F, 5);
    }

    public void tick(boolean acceptPressed, boolean declinePressed) {
        if (call == null) return;
        float now = HudTime.ticks();
        if (outcome != Outcome.RINGING) {
            if (now - endedAt > OUTRO) call = null;
            return;
        }
        if (acceptPressed) end(Outcome.ACCEPTED);
        else if (declinePressed) end(Outcome.DECLINED);
        else if (now - startedAt > call.timeoutTicks()) end(Outcome.MISSED);
    }

    private void end(Outcome how) {
        outcome = how;
        endedAt = HudTime.ticks();
        switch (how) {
            case ACCEPTED -> call.onAccept().run();
            case DECLINED -> call.onDecline().run();
            case MISSED -> {
                call.onDecline().run();
                ChromeHud.notify(NotificationKind.WARNING, Component.translatable("chromeneon.hud.call.missed"), call.caller());
            }
            default -> {}
        }
    }

    /** Whether the popup holds the answer keys right now. */
    public boolean ringing() {
        return call != null && outcome == Outcome.RINGING;
    }

    public IncomingCall call() {
        return call;
    }

    public Outcome outcome() {
        return outcome;
    }

    public float startedAt() {
        return startedAt;
    }

    public float endedAt() {
        return endedAt;
    }

    public void clear() {
        call = null;
    }
}
