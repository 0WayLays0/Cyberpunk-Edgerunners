package com.waylays.chromeneon.common.hud;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

/**
 * What a HUD notification is about. Shared by both sides: the server picks a kind, the client gives it its look
 * (colour, icon, arrival effect). Priority decides which notifications stay on screen when there are too many.
 */
public enum NotificationKind implements StringRepresentable {
    INFO(0, "info", 1, 100),
    SYSTEM_ON(1, "system_on", 2, 90),
    SYSTEM_OFF(2, "system_off", 2, 90),
    CYBERWARE(3, "cyberware", 3, 120),
    SCAN(4, "scan", 1, 80),
    WARNING(5, "warning", 4, 110),
    ALERT(6, "alert", 5, 120),
    ERROR(7, "error", 5, 120);

    private static final IntFunction<NotificationKind> BY_ID = ByIdMap.continuous(k -> k.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, NotificationKind> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, k -> k.id);
    public static final Codec<NotificationKind> CODEC = StringRepresentable.fromEnum(NotificationKind::values);

    private final int id;
    private final String name;
    private final int priority;
    private final int defaultDuration;

    NotificationKind(int id, String name, int priority, int defaultDuration) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.defaultDuration = defaultDuration;
    }

    public int priority() {
        return priority;
    }

    /** Ticks on screen when the sender does not choose. */
    public int defaultDuration() {
        return defaultDuration;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
