package com.waylays.chromeneon.common.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.waylays.chromeneon.common.cyberware.CyberwareLoadout;
import com.waylays.chromeneon.common.hacking.RamPool;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything persistent about a player's cyberware, attached to the player ({@code chromeneon:cyber_data}).
 * Humanity is not stored: it is derived from the installed implants, so it can never drift out of sync with them.
 * Fields are added with defaults ({@code optionalFieldOf}) so older saves keep loading as the mod grows.
 */
public final class PlayerCyberData {
    public static final Codec<PlayerCyberData> CODEC = RecordCodecBuilder.create(i -> i.group(
            CyberwareLoadout.CODEC.optionalFieldOf("loadout", new CyberwareLoadout()).forGetter(d -> d.loadout),
            Codec.INT.optionalFieldOf("ram", 0).forGetter(d -> d.ram.cells()),
            ResourceLocation.CODEC.listOf().optionalFieldOf("disabled", List.of()).forGetter(d -> List.copyOf(d.disabled))
    ).apply(i, PlayerCyberData::new));

    private final CyberwareLoadout loadout;
    private final RamPool ram;
    private final Set<ResourceLocation> disabled;

    public PlayerCyberData() {
        this(new CyberwareLoadout(), 0, List.of());
    }

    private PlayerCyberData(CyberwareLoadout loadout, int ram, List<ResourceLocation> disabled) {
        this.loadout = loadout.copy();
        this.ram = new RamPool(ram);
        this.disabled = new HashSet<>(disabled);
    }

    public CyberwareLoadout loadout() {
        return loadout;
    }

    public RamPool ram() {
        return ram;
    }

    public boolean isEnabled(ResourceLocation implant) {
        return loadout.contains(implant) && !disabled.contains(implant);
    }

    /** Switches an installed implant on or off. Returns false if it is not installed. */
    public boolean setEnabled(ResourceLocation implant, boolean enabled) {
        if (!loadout.contains(implant)) return false;
        if (enabled) disabled.remove(implant); else disabled.add(implant);
        return true;
    }

    public boolean uninstall(ResourceLocation implant) {
        disabled.remove(implant);
        return loadout.remove(implant);
    }

    public Set<ResourceLocation> disabled() {
        return Set.copyOf(disabled);
    }
}
