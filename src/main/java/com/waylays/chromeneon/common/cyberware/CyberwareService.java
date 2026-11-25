package com.waylays.chromeneon.common.cyberware;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.implants.ArgusOptics;
import com.waylays.chromeneon.common.hud.HudMessages;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.item.ImplantItem;
import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.player.PlayerCyberData;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

/**
 * Every change to a player's cyberware goes through here, on the server, checked in full: what the client sends
 * is a request, never a fact. Surgery (install, removal) needs the player seated in a ripperdoc chair and out of
 * combat; switching implants on or off and using them can be done anywhere.
 */
public final class CyberwareService {
    private CyberwareService() {}

    private static int maxHumanity() {
        return ServerConfig.MAX_HUMANITY.get();
    }

    public static int capacity(PlayerCyberData data) {
        return ServerConfig.BASE_CAPACITY.get() + data.capacityBonus();
    }

    /** Surgery preconditions shared by install and removal. */
    private static InstallResult surgeryAllowed(ServerPlayer player, PlayerCyberData data, boolean seated) {
        if (!seated) return InstallResult.NOT_SEATED;
        if (ServerConfig.inCombat(player, data.lastHurtAt)) return InstallResult.IN_COMBAT;
        return InstallResult.OK;
    }

    /** Installs the implant carried in inventory slot {@code inventorySlot} into {@code slot}. */
    public static InstallResult install(ServerPlayer player, boolean seated, ResourceLocation slot, int inventorySlot) {
        PlayerCyberData data = CyberData.get(player);
        InstallResult allowed = surgeryAllowed(player, data, seated);
        if (!allowed.ok()) return allowed;
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize()) return InstallResult.NO_ITEM;
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        if (!(stack.getItem() instanceof ImplantItem item)) return InstallResult.NO_ITEM;
        ResourceLocation implantId = ModImplants.id(item.implant());
        InstallResult result = data.loadout().install(implantId, slot, CyberwareCatalog.REGISTRIES, maxHumanity(), capacity(data));
        if (!result.ok()) return result;
        stack.shrink(1);
        int index = data.loadout().in(slot).size() - 1;
        InstalledImplant installed = data.loadout().get(slot, index).orElseThrow();
        ImplantContext ctx = new ImplantContext(player, data, slot, index, installed, player.level().getGameTime());
        item.implant().onInstalled(ctx);
        player.level().playSound(null, player.blockPosition(), ModSounds.IMPLANT_INSTALL.get(), SoundSource.PLAYERS, 1F, 1F);
        HudMessages.notify(player, NotificationKind.CYBERWARE, Component.translatable("chromeneon.notify.cyberware.installed"),
                Component.translatable("chromeneon.implant." + implantId.getPath()));
        NeoForge.EVENT_BUS.post(new CyberwareEvent.Installed(player, implantId, slot));
        CyberData.sync(player);
        return InstallResult.OK;
    }

    /** Removes the implant in {@code slot} at {@code index} and gives its item back. */
    public static InstallResult remove(ServerPlayer player, boolean seated, ResourceLocation slot, int index) {
        PlayerCyberData data = CyberData.get(player);
        InstallResult allowed = surgeryAllowed(player, data, seated);
        if (!allowed.ok()) return allowed;
        Optional<InstalledImplant> present = data.loadout().get(slot, index);
        if (present.isEmpty()) return InstallResult.NOT_INSTALLED;
        Optional<Implant> implant = ModImplants.get(present.get().id());
        ItemStack back = implant.flatMap(ImplantItem::stackFor).orElse(ItemStack.EMPTY);
        if (!back.isEmpty() && player.getInventory().getFreeSlot() < 0) return InstallResult.INVENTORY_FULL;
        ImplantContext ctx = new ImplantContext(player, data, slot, index, present.get(), player.level().getGameTime());
        implant.ifPresent(i -> i.onRemoved(ctx));
        data.loadout().remove(slot, index);
        if (!back.isEmpty()) player.getInventory().add(back);
        player.level().playSound(null, player.blockPosition(), ModSounds.IMPLANT_REMOVE.get(), SoundSource.PLAYERS, 1F, 1F);
        HudMessages.notify(player, NotificationKind.SYSTEM_OFF, Component.translatable("chromeneon.notify.cyberware.removed"),
                Component.translatable("chromeneon.implant." + present.get().id().getPath()));
        NeoForge.EVENT_BUS.post(new CyberwareEvent.Removed(player, present.get().id(), slot));
        CyberData.sync(player);
        return InstallResult.OK;
    }

    /**
     * Installs without a chair or an item: for operators' commands and tests. The body's rules still apply.
     */
    public static InstallResult grant(ServerPlayer player, ResourceLocation implantId, ResourceLocation slot) {
        PlayerCyberData data = CyberData.get(player);
        InstallResult result = data.loadout().install(implantId, slot, CyberwareCatalog.REGISTRIES, maxHumanity(), capacity(data));
        if (!result.ok()) return result;
        int index = data.loadout().in(slot).size() - 1;
        InstalledImplant installed = data.loadout().get(slot, index).orElseThrow();
        ModImplants.get(implantId).ifPresent(i -> i.onInstalled(new ImplantContext(player, data, slot, index, installed, player.level().getGameTime())));
        NeoForge.EVENT_BUS.post(new CyberwareEvent.Installed(player, implantId, slot));
        CyberData.sync(player);
        return InstallResult.OK;
    }

    /** Removes without a chair and without giving the item back: for operators' commands and tests. */
    public static InstallResult revoke(ServerPlayer player, ResourceLocation slot, int index) {
        PlayerCyberData data = CyberData.get(player);
        Optional<InstalledImplant> present = data.loadout().get(slot, index);
        if (present.isEmpty()) return InstallResult.NOT_INSTALLED;
        ImplantContext ctx = new ImplantContext(player, data, slot, index, present.get(), player.level().getGameTime());
        ModImplants.get(present.get().id()).ifPresent(i -> i.onRemoved(ctx));
        data.loadout().remove(slot, index);
        NeoForge.EVENT_BUS.post(new CyberwareEvent.Removed(player, present.get().id(), slot));
        CyberData.sync(player);
        return InstallResult.OK;
    }

    /** Switches an installed implant on or off. Off, it does nothing and gives nothing (RAM bonus included). */
    public static InstallResult setEnabled(ServerPlayer player, ResourceLocation slot, int index, boolean enabled) {
        PlayerCyberData data = CyberData.get(player);
        Optional<InstalledImplant> present = data.loadout().get(slot, index);
        if (present.isEmpty()) return InstallResult.NOT_INSTALLED;
        ImplantState state = present.get().state();
        if (state.enabled == enabled) return InstallResult.OK;
        state.enabled = enabled;
        if (!enabled) {
            ImplantContext ctx = new ImplantContext(player, data, slot, index, present.get(), player.level().getGameTime());
            ModImplants.get(present.get().id()).ifPresent(i -> i.onStopped(ctx));
        }
        player.level().playSound(null, player.blockPosition(), (enabled ? ModSounds.SYSTEM_ON : ModSounds.SYSTEM_OFF).get(),
                SoundSource.PLAYERS, 0.7F, 1F);
        CyberData.sync(player);
        return InstallResult.OK;
    }

    /** Uses the first installed implant of a kind (keys), and reports the outcome. */
    public static UseResult use(ServerPlayer player, Implant implant) {
        Optional<ImplantContext> ctx = Cyberware.first(player, implant);
        if (ctx.isEmpty()) return UseResult.NOT_INSTALLED;
        UseResult result = implant.use(ctx.get());
        if (result.ok()) NeoForge.EVENT_BUS.post(new CyberwareEvent.Used(player, ctx.get().installed().id(), ctx.get().slot()));
        CyberData.sync(player);
        return result;
    }

    /** Uses every installed copy of an implant together (both blade sets deploy at once). */
    public static UseResult useAll(ServerPlayer player, Implant implant) {
        var all = Cyberware.of(player, implant);
        if (all.isEmpty()) return UseResult.NOT_INSTALLED;
        boolean target = !all.getFirst().state().active;
        UseResult last = UseResult.OK;
        for (ImplantContext ctx : all) {
            if (ctx.state().active == target) continue;
            last = implant.use(ctx);
        }
        CyberData.sync(player);
        return last;
    }

    /** The optics' tactical analysis. */
    public static UseResult analyse(ServerPlayer player) {
        Optional<ImplantContext> ctx = Cyberware.first(player, ModImplants.ARGUS_OPTICS.get());
        if (ctx.isEmpty()) return UseResult.NOT_INSTALLED;
        UseResult result = ((ArgusOptics) ModImplants.ARGUS_OPTICS.get()).analyse(ctx.get());
        CyberData.sync(player);
        return result;
    }
}
