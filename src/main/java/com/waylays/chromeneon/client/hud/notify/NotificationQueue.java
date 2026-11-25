package com.waylays.chromeneon.client.hud.notify;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Which notifications are on screen. Waiting ones go up by priority (first come first served within a priority);
 * when the screen is full, a more important one pushes out the least important visible one. Expired ones leave
 * with an animation before freeing their place. Pure logic over HUD time, tested without the game.
 */
public final class NotificationQueue {
    public static final float ENTER = 8F, LEAVE = 7F;

    private final List<Notification> pending = new ArrayList<>();
    private final List<Notification> visible = new ArrayList<>();

    public void push(Notification n, float now) {
        for (Notification v : visible) {
            if (!v.leaving() && v.sameAs(n)) {
                v.extend(now);
                return;
            }
        }
        if (pending.stream().anyMatch(p -> p.sameAs(n))) return;
        pending.add(n);
        pending.sort(Comparator.comparingInt((Notification p) -> p.priority).reversed());
    }

    public void tick(float now, int maxVisible) {
        for (Notification v : visible) {
            if (!v.leaving() && (maxVisible <= 0 || v.expired(now))) v.leaveAt = now;
        }
        visible.removeIf(v -> v.leaving() && now - v.leaveAt >= LEAVE);
        if (maxVisible <= 0) {
            pending.clear();
            return;
        }
        // Fewer places than cards (the screen got smaller): the least important leave first
        long shown = visible.stream().filter(v -> !v.leaving()).count();
        while (shown > maxVisible) {
            visible.stream().filter(v -> !v.leaving())
                    .min(Comparator.comparingInt((Notification v) -> v.priority).thenComparingDouble(v -> v.shownAt))
                    .ifPresent(v -> v.leaveAt = now);
            shown--;
        }
        while (!pending.isEmpty()) {
            Notification next = pending.getFirst();
            long active = visible.stream().filter(v -> !v.leaving()).count();
            if (active < maxVisible) {
                pending.removeFirst();
                next.show(now);
                visible.add(next);
                continue;
            }
            visible.stream().filter(v -> !v.leaving())
                    .min(Comparator.comparingInt((Notification v) -> v.priority).thenComparingDouble(v -> v.shownAt))
                    .filter(lowest -> lowest.priority < next.priority)
                    .ifPresent(lowest -> lowest.leaveAt = now);
            break;
        }
    }

    /** On screen, in arrival order (leaving ones included until their exit ends). */
    public List<Notification> visible() {
        return visible;
    }

    public int waiting() {
        return pending.size();
    }

    public void clear() {
        pending.clear();
        visible.clear();
    }
}
