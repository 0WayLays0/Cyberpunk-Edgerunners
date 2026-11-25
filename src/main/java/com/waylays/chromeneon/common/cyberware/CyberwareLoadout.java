package com.waylays.chromeneon.common.cyberware;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The implants in a body, slot by slot, and the rules for installing them: the slot must accept the implant and
 * have a free socket, the implant must not exceed its count per body nor conflict with an installed one, and the
 * body must have the humanity and the cyberware capacity it costs. Pure logic over a {@link CyberwareCatalog}.
 */
public final class CyberwareLoadout {
    public static final Codec<CyberwareLoadout> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, InstalledImplant.CODEC.listOf())
            .xmap(CyberwareLoadout::new, l -> l.slots);

    /** One installed implant with where it sits. */
    public record Entry(ResourceLocation slot, int index, InstalledImplant installed) {}

    private final Map<ResourceLocation, List<InstalledImplant>> slots = new LinkedHashMap<>();

    public CyberwareLoadout() {}

    private CyberwareLoadout(Map<ResourceLocation, List<InstalledImplant>> from) {
        from.forEach((slot, list) -> {
            List<InstalledImplant> copy = new ArrayList<>();
            list.forEach(i -> copy.add(i.copy()));
            slots.put(slot, copy);
        });
    }

    public CyberwareLoadout copy() {
        return new CyberwareLoadout(slots);
    }

    public List<InstalledImplant> in(ResourceLocation slot) {
        return Collections.unmodifiableList(slots.getOrDefault(slot, List.of()));
    }

    public List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        slots.forEach((slot, list) -> {
            for (int i = 0; i < list.size(); i++) out.add(new Entry(slot, i, list.get(i)));
        });
        return out;
    }

    public Optional<InstalledImplant> get(ResourceLocation slot, int index) {
        List<InstalledImplant> list = slots.get(slot);
        return list == null || index < 0 || index >= list.size() ? Optional.empty() : Optional.of(list.get(index));
    }

    public int count(ResourceLocation implant) {
        return (int) entries().stream().filter(e -> e.installed().id().equals(implant)).count();
    }

    public boolean contains(ResourceLocation implant) {
        return count(implant) > 0;
    }

    public int humanityCost(CyberwareCatalog catalog) {
        return entries().stream().map(e -> catalog.implant(e.installed().id())).flatMap(Optional::stream)
                .mapToInt(i -> i.properties().humanityCost()).sum();
    }

    public int capacityUsed(CyberwareCatalog catalog) {
        return entries().stream().map(e -> catalog.implant(e.installed().id())).flatMap(Optional::stream)
                .mapToInt(i -> i.properties().capacityCost()).sum();
    }

    /** RAM added by the implants that work at {@code now}. */
    public int ramBonus(CyberwareCatalog catalog, long now) {
        return entries().stream().filter(e -> e.installed().state().working(now))
                .map(e -> catalog.implant(e.installed().id())).flatMap(Optional::stream).mapToInt(Implant::ramBonus).sum();
    }

    public InstallResult check(ResourceLocation implantId, ResourceLocation slotId, CyberwareCatalog catalog,
                               int maxHumanity, int capacity) {
        Optional<Implant> found = catalog.implant(implantId);
        if (found.isEmpty()) return InstallResult.UNKNOWN_IMPLANT;
        Optional<BodySlot> slot = catalog.slot(slotId);
        if (slot.isEmpty()) return InstallResult.UNKNOWN_SLOT;
        Implant.Properties p = found.get().properties();
        if (!p.slots().contains(slotId)) return InstallResult.WRONG_SLOT;
        if (count(implantId) >= p.maxPerBody()) return InstallResult.ALREADY_INSTALLED;
        if (in(slotId).size() >= slot.get().sockets()) return InstallResult.SLOT_FULL;
        if (p.exclusiveGroup().isPresent()) {
            boolean clash = entries().stream().map(e -> catalog.implant(e.installed().id())).flatMap(Optional::stream)
                    .anyMatch(o -> o.properties().exclusiveGroup().equals(p.exclusiveGroup()));
            if (clash) return InstallResult.CONFLICT;
        }
        if (humanityCost(catalog) + p.humanityCost() > maxHumanity) return InstallResult.NOT_ENOUGH_HUMANITY;
        if (capacityUsed(catalog) + p.capacityCost() > capacity) return InstallResult.NOT_ENOUGH_CAPACITY;
        return InstallResult.OK;
    }

    /** Installs after {@link #check}; a refused install changes nothing. */
    public InstallResult install(ResourceLocation implantId, ResourceLocation slotId, CyberwareCatalog catalog,
                                 int maxHumanity, int capacity) {
        InstallResult result = check(implantId, slotId, catalog, maxHumanity, capacity);
        if (result.ok()) slots.computeIfAbsent(slotId, s -> new ArrayList<>()).add(new InstalledImplant(implantId, new ImplantState()));
        return result;
    }

    /** Takes the implant out of its socket; empty when there is none there. */
    public Optional<InstalledImplant> remove(ResourceLocation slot, int index) {
        List<InstalledImplant> list = slots.get(slot);
        if (list == null || index < 0 || index >= list.size()) return Optional.empty();
        InstalledImplant removed = list.remove(index);
        if (list.isEmpty()) slots.remove(slot);
        return Optional.of(removed);
    }
}
