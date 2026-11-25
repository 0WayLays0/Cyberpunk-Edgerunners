package com.waylays.chromeneon.client.screen;

import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.cyberware.CyberwareCatalog;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.InstallResult;
import com.waylays.chromeneon.common.entity.SeatEntity;
import com.waylays.chromeneon.common.item.ImplantItem;
import com.waylays.chromeneon.common.network.CyberwareRequestPayload;
import com.waylays.chromeneon.common.network.SyncCyberStatePayload;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The ripperdoc's interface, open while seated in the chair: everything of the cyberware interface, plus the
 * implants the player carries, the slots that accept the chosen one, the verdict of the body's rules for the chosen
 * socket, and the install and remove actions. The verdict shown here is computed with the same rules the server
 * applies; the server decides.
 */
public class RipperdocScreen extends CyberwareScreen {
    private final BlockPos chair;
    private int chosenItem = -1;

    public RipperdocScreen(BlockPos chair) {
        super(Component.translatable("chromeneon.screen.ripperdoc"));
        this.chair = chair;
    }

    @Override
    protected String header() {
        return Component.translatable("chromeneon.screen.ripperdoc").getString();
    }

    private ImplantItem chosen() {
        var player = Minecraft.getInstance().player;
        if (player == null || chosenItem < 0) return null;
        ItemStack stack = player.getInventory().getItem(chosenItem);
        return stack.getItem() instanceof ImplantItem item ? item : null;
    }

    @Override
    protected boolean highlightSlot(ResourceLocation slot) {
        ImplantItem item = chosen();
        return item != null && item.implant().properties().slots().contains(slot);
    }

    @Override
    protected void emptySocket(GuiGraphics g, SyncCyberStatePayload state, float x, float y, float w) {
        ImplantItem item = chosen();
        if (item == null) {
            super.emptySocket(g, state, x, y, w);
            HudText.draw(g, HudText.label(Component.translatable("chromeneon.screen.choose_implant")), x, y + 28F, NeonPalette.CYAN, px);
            return;
        }
        Implant implant = item.implant();
        float ly = sheet(g, implant, item.implantId(), x, y, w);
        InstallResult verdict = state.loadout().check(item.implantId(), selected.slot(), CyberwareCatalog.REGISTRIES,
                state.stats().maxHumanity(), state.stats().capacity());
        boolean seated = Minecraft.getInstance().player.getVehicle() instanceof SeatEntity;
        if (verdict.ok() && !seated) verdict = InstallResult.NOT_SEATED;
        Component v = HudText.data(Component.translatable(verdict.translationKey(), Component.translatable("chromeneon.implant." + item.implantId().getPath())));
        HudText.draw(g, v, x, ly + 2F, verdict.ok() ? NeonPalette.CYAN : NeonPalette.ORANGE, px);
        Socket at = selected;
        int slot = chosenItem;
        buttons.add(new NeonButton(x, ly + 12F, 70F, 9F, Component.translatable("chromeneon.screen.install"), NeonPalette.CYAN,
                verdict.ok(), () -> request(CyberwareRequestPayload.Action.INSTALL, at, slot, false)));
    }

    @Override
    protected void actions(GuiGraphics g, Socket socket, float x, float y, boolean installed) {
        buttons.add(new NeonButton(x, y, 58F, 9F, Component.translatable("chromeneon.screen.remove"), NeonPalette.RED, true,
                () -> request(CyberwareRequestPayload.Action.REMOVE, socket, -1, false)));
    }

    /** The implants carried, at the bottom: choosing one shows where it fits and selects its first free socket. */
    @Override
    protected void extra(GuiGraphics g, SyncCyberStatePayload state, int mx, int my) {
        var player = Minecraft.getInstance().player;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() instanceof ImplantItem) slots.add(i);
        }
        float x = width * 0.64F, y = height - 14F - 10F - Math.min(6, Math.max(1, slots.size())) * 10F - 12F;
        HudText.draw(g, HudText.label(Component.translatable("chromeneon.screen.carried")), x, y, NeonPalette.CYAN_SOFT, px);
        HudDraw.rect(g, x, y + 6F, width - 14F, y + 6F + px, NeonPalette.alpha(NeonPalette.CYAN, 0.4F));
        if (slots.isEmpty()) {
            HudText.draw(g, HudText.label(Component.translatable("chromeneon.screen.none_carried")), x, y + 10F, NeonPalette.LAVENDER, px);
        }
        for (int n = 0; n < slots.size() && n < 6; n++) {
            int inv = slots.get(n);
            ImplantItem item = (ImplantItem) player.getInventory().getItem(inv).getItem();
            float ry = y + 10F + n * 10F;
            boolean sel = inv == chosenItem;
            Component name = HudText.of(HudText.Kind.BODY, Component.translatable("chromeneon.implant." + item.implantId().getPath()));
            buttons.add(new NeonButton(x, ry, width - 14F - x - 4F, 8F, Component.empty(), sel ? NeonPalette.CYAN : NeonPalette.VIOLET, true, () -> {
                chosenItem = inv;
                selectFreeSocket(state, item);
                play(ModSounds.UI_CLICK.get());
            }));
            HudText.draw(g, name, x + 5F, ry + 1F, NeonPalette.WHITE, px);
            Implant.Properties p = item.implant().properties();
            HudText.drawRight(g, HudText.label("H" + p.humanityCost() + "  C" + p.capacityCost()), width - 22F, ry + 1.5F, NeonPalette.LAVENDER, px);
        }
        boolean seated = player.getVehicle() instanceof SeatEntity;
        Component chairState = HudText.label(Component.translatable(seated ? "chromeneon.screen.seated" : "chromeneon.screen.not_seated",
                chair.getX(), chair.getY(), chair.getZ()));
        HudText.draw(g, chairState, 14F, 36F, seated ? NeonPalette.CYAN : NeonPalette.ORANGE, px);
    }

    private void selectFreeSocket(SyncCyberStatePayload state, ImplantItem item) {
        for (ResourceLocation slot : item.implant().properties().slots()) {
            var body = ModBodySlots.REGISTRY.get(slot);
            if (body != null && state.loadout().in(slot).size() < body.sockets()) {
                selected = new Socket(slot, state.loadout().in(slot).size());
                return;
            }
        }
        ResourceLocation first = item.implant().properties().slots().iterator().next();
        selected = new Socket(first, 0);
    }

    /** For the automated test: chooses the implant carried in an inventory slot, as a click would. */
    public void choose(int inventorySlot) {
        chosenItem = inventorySlot;
        var player = Minecraft.getInstance().player;
        if (player != null && player.getInventory().getItem(inventorySlot).getItem() instanceof ImplantItem item
                && com.waylays.chromeneon.common.player.SyncedCyberState.current() != null) {
            selectFreeSocket(com.waylays.chromeneon.common.player.SyncedCyberState.current(), item);
        }
    }

    /** For the automated test: presses the install button for the current choice. */
    public void installChosen() {
        if (selected != null && chosenItem >= 0) request(CyberwareRequestPayload.Action.INSTALL, selected, chosenItem, false);
    }
}
