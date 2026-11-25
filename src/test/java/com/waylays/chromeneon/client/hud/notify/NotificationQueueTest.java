package com.waylays.chromeneon.client.hud.notify;

import com.waylays.chromeneon.common.hud.NotificationKind;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationQueueTest {
    private static Notification n(NotificationKind kind, String title, int duration) {
        return Notification.builder(kind, Component.literal(title)).duration(duration).build();
    }

    @Test
    void showsUpToTheLimitThenWaits() {
        NotificationQueue q = new NotificationQueue();
        for (int i = 0; i < 4; i++) q.push(n(NotificationKind.INFO, "n" + i, 100), 0);
        q.tick(0, 3);
        assertEquals(3, q.visible().size());
        assertEquals(1, q.waiting());
    }

    @Test
    void expiredLeaveWithAnAnimationThenFreeTheirPlace() {
        NotificationQueue q = new NotificationQueue();
        q.push(n(NotificationKind.INFO, "a", 20), 0);
        q.push(n(NotificationKind.INFO, "b", 20), 0);
        q.tick(0, 1);
        q.tick(20, 1);
        assertTrue(q.visible().getFirst().leaving(), "expired: leaving");
        assertEquals(2, q.visible().size(), "still drawn while it leaves, and the next one already enters");
        assertEquals("b", q.visible().get(1).title().getString());
        q.tick(20 + NotificationQueue.LEAVE, 1);
        assertEquals(1, q.visible().size(), "gone after its exit animation");
        assertEquals("b", q.visible().getFirst().title().getString());
    }

    @Test
    void higherPriorityPushesOutTheLeastImportant() {
        NotificationQueue q = new NotificationQueue();
        q.push(n(NotificationKind.INFO, "info", 1000), 0);
        q.tick(0, 2);
        q.push(n(NotificationKind.SCAN, "scan", 1000), 3);
        q.tick(3, 2);
        q.push(n(NotificationKind.ALERT, "alert", 100), 5);
        q.tick(5, 2);
        assertTrue(q.visible().stream().anyMatch(v -> v.title().getString().equals("info") && v.leaving()),
                "info and scan share the lowest priority: the older one leaves");
        assertTrue(q.visible().stream().anyMatch(v -> v.title().getString().equals("scan") && !v.leaving()));
        q.tick(5 + NotificationQueue.LEAVE, 2);
        assertTrue(q.visible().stream().anyMatch(v -> v.title().getString().equals("alert")));
    }

    @Test
    void aRepeatExtendsInsteadOfStacking() {
        NotificationQueue q = new NotificationQueue();
        q.push(n(NotificationKind.WARNING, "low ram", 40), 0);
        q.tick(0, 3);
        q.push(n(NotificationKind.WARNING, "low ram", 40), 30);
        q.tick(30, 3);
        assertEquals(1, q.visible().size());
        q.tick(45, 3);
        assertFalse(q.visible().getFirst().leaving(), "extended to tick 70");
    }

    @Test
    void progressNotificationsStayUntilDone() {
        NotificationQueue q = new NotificationQueue();
        AtomicReference<Double> p = new AtomicReference<>(0.0);
        q.push(Notification.builder(NotificationKind.SCAN, Component.literal("upload")).duration(20).progress(p::get).build(), 0);
        q.tick(0, 3);
        q.tick(500, 3);
        assertFalse(q.visible().getFirst().leaving(), "not done");
        p.set(1.0);
        q.tick(501, 3);
        q.tick(540, 3);
        assertTrue(q.visible().getFirst().leaving(), "done, then lingered");
    }

    @Test
    void lessRoomSendsTheLeastImportantAway() {
        NotificationQueue q = new NotificationQueue();
        q.push(n(NotificationKind.ALERT, "alert", 1000), 0);
        q.push(n(NotificationKind.INFO, "info", 1000), 0);
        q.push(n(NotificationKind.WARNING, "warn", 1000), 0);
        q.tick(0, 3);
        q.tick(1, 2);
        assertTrue(q.visible().stream().anyMatch(v -> v.title().getString().equals("info") && v.leaving()));
        assertEquals(2, q.visible().stream().filter(v -> !v.leaving()).count());
    }

    @Test
    void zeroVisibleTurnsThemOff() {
        NotificationQueue q = new NotificationQueue();
        q.push(n(NotificationKind.INFO, "x", 100), 0);
        q.tick(0, 0);
        assertTrue(q.visible().isEmpty());
        assertEquals(0, q.waiting());
    }
}
