package com.waylays.chromeneon.client.hud.notify;

import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.common.hud.HudNotifyPayload;
import com.waylays.chromeneon.common.hud.NotificationKind;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleSupplier;

/**
 * One HUD notification: what it says, its kind (look), priority and duration. A notification with a progress
 * source (an upload, a transfer) stays until the progress reaches 1, then lingers briefly.
 */
public final class Notification {
    final NotificationKind kind;
    final Component title;
    final Component message;
    final int priority;
    final HudIcon icon;
    final DoubleSupplier progress;
    private final int duration;

    float shownAt = Float.NaN, leaveAt = Float.NaN, doneAt = Float.NaN, expiresAt = Float.NaN;

    private Notification(Builder b) {
        this.kind = b.kind;
        this.title = b.title;
        this.message = b.message;
        this.priority = b.priority;
        this.icon = b.icon;
        this.progress = b.progress;
        this.duration = b.duration;
    }

    public static Builder builder(NotificationKind kind, Component title) {
        return new Builder(kind, title);
    }

    public static Notification of(HudNotifyPayload payload) {
        return builder(payload.kind(), payload.title()).message(payload.message()).duration(payload.durationTicks()).build();
    }

    void show(float now) {
        shownAt = now;
        expiresAt = now + duration;
    }

    boolean leaving() {
        return !Float.isNaN(leaveAt);
    }

    boolean expired(float now) {
        if (progress != null) {
            if (progress.getAsDouble() < 1.0) return false;
            if (Float.isNaN(doneAt)) doneAt = now;
            return now - doneAt > 30F;
        }
        return now >= expiresAt;
    }

    /** Same kind and text: a repeat extends the one on screen instead of stacking a copy. */
    boolean sameAs(Notification other) {
        return kind == other.kind && title.equals(other.title) && message.equals(other.message) && progress == null;
    }

    void extend(float now) {
        expiresAt = Math.max(expiresAt, now + duration);
    }

    public NotificationKind kind() {
        return kind;
    }

    public Component title() {
        return title;
    }

    public static final class Builder {
        private final NotificationKind kind;
        private final Component title;
        private Component message = Component.empty();
        private int priority;
        private int duration;
        private HudIcon icon;
        private DoubleSupplier progress;

        private Builder(NotificationKind kind, Component title) {
            this.kind = kind;
            this.title = title;
            this.priority = kind.priority();
            this.duration = kind.defaultDuration();
        }

        public Builder message(Component message) {
            this.message = message;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder duration(int ticks) {
            this.duration = Math.max(20, ticks);
            return this;
        }

        /** Overrides the kind's icon. */
        public Builder icon(HudIcon icon) {
            this.icon = icon;
            return this;
        }

        /** 0..1, polled every frame; the notification stays until it reaches 1. */
        public Builder progress(DoubleSupplier progress) {
            this.progress = progress;
            return this;
        }

        public Notification build() {
            return new Notification(this);
        }
    }
}
