package com.waylays.chromeneon.common.cyberware;

import com.waylays.chromeneon.common.registry.ModBodySlots;
import net.minecraft.resources.ResourceLocation;

/**
 * A place in the body that holds cyberware, registered in {@code chromeneon:body_slot} (other mods can add their
 * own). It has a number of sockets and a region, used to draw it on the body diagram.
 */
public final class BodySlot {
    /** Where the slot sits on the body diagram. */
    public enum Region { HEAD, EYES, SPINE, HEART, SKIN, LEFT_ARM, RIGHT_ARM, LEGS }

    private final int sockets;
    private final Region region;

    public BodySlot(int sockets, Region region) {
        if (sockets < 1) throw new IllegalArgumentException("a body slot needs at least one socket");
        this.sockets = sockets;
        this.region = region;
    }

    public int sockets() {
        return sockets;
    }

    public Region region() {
        return region;
    }

    public ResourceLocation id() {
        return ModBodySlots.REGISTRY.getKey(this);
    }

    public static String translationKey(ResourceLocation id) {
        return "chromeneon.slot." + id.getNamespace() + "." + id.getPath();
    }
}
