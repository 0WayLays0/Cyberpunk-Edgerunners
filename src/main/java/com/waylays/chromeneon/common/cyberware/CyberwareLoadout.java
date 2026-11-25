package com.waylays.chromeneon.common.cyberware;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The implants installed in a body, by slot, and the rules that govern installing them. Implants are stored by id
 * and resolved through a lookup, so an implant whose mod was removed is kept (not silently deleted) but ignored.
 */
public final class CyberwareLoadout {
    public static final Codec<CyberwareLoadout> CODEC = Codec.unboundedMap(BodySlot.CODEC, ResourceLocation.CODEC.listOf())
            .xmap(CyberwareLoadout::new, l -> l.installed);

    private final EnumMap<BodySlot, List<ResourceLocation>> installed = new EnumMap<>(BodySlot.class);

    public CyberwareLoadout() {}

    private CyberwareLoadout(Map<BodySlot, List<ResourceLocation>> from) {
        from.forEach((slot, ids) -> installed.put(slot, new ArrayList<>(ids)));
    }

    public CyberwareLoadout copy() {
        return new CyberwareLoadout(installed);
    }

    public List<ResourceLocation> in(BodySlot slot) {
        return Collections.unmodifiableList(installed.getOrDefault(slot, List.of()));
    }

    public boolean contains(ResourceLocation id) {
        return installed.values().stream().anyMatch(ids -> ids.contains(id));
    }

    public List<ResourceLocation> all() {
        return installed.values().stream().flatMap(List::stream).toList();
    }

    public int humanityCost(Function<ResourceLocation, Optional<Implant>> lookup) {
        return all().stream().map(lookup).flatMap(Optional::stream).mapToInt(Implant::humanityCost).sum();
    }

    public InstallResult check(ResourceLocation id, Function<ResourceLocation, Optional<Implant>> lookup, int maxHumanity) {
        Optional<Implant> found = lookup.apply(id);
        if (found.isEmpty()) return InstallResult.UNKNOWN_IMPLANT;
        Implant implant = found.get();
        if (contains(id)) return InstallResult.ALREADY_INSTALLED;
        if (in(implant.slot()).size() >= implant.slot().sockets()) return InstallResult.SLOT_FULL;
        if (implant.exclusiveGroup().isPresent()) {
            boolean clash = all().stream().map(lookup).flatMap(Optional::stream)
                    .anyMatch(other -> other.exclusiveGroup().equals(implant.exclusiveGroup()));
            if (clash) return InstallResult.CONFLICT;
        }
        if (humanityCost(lookup) + implant.humanityCost() > maxHumanity) return InstallResult.NOT_ENOUGH_HUMANITY;
        return InstallResult.OK;
    }

    /** Installs after {@link #check}; returns the check result so a refused install changes nothing. */
    public InstallResult install(ResourceLocation id, Function<ResourceLocation, Optional<Implant>> lookup, int maxHumanity) {
        InstallResult result = check(id, lookup, maxHumanity);
        if (result.ok()) {
            installed.computeIfAbsent(lookup.apply(id).orElseThrow().slot(), s -> new ArrayList<>()).add(id);
        }
        return result;
    }

    public boolean remove(ResourceLocation id) {
        return installed.values().stream().anyMatch(ids -> ids.remove(id));
    }
}
