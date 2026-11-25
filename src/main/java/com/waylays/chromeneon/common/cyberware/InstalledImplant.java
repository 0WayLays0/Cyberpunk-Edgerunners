package com.waylays.chromeneon.common.cyberware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** An implant in a socket: which implant, and its state. */
public final class InstalledImplant {
    public static final Codec<InstalledImplant> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(InstalledImplant::id),
            ImplantState.CODEC.optionalFieldOf("state", new ImplantState()).forGetter(InstalledImplant::state)
    ).apply(i, InstalledImplant::new));

    private final ResourceLocation id;
    private final ImplantState state;

    public InstalledImplant(ResourceLocation id, ImplantState state) {
        this.id = id;
        this.state = state;
    }

    public ResourceLocation id() {
        return id;
    }

    public ImplantState state() {
        return state;
    }

    public InstalledImplant copy() {
        return new InstalledImplant(id, state.copy());
    }
}
