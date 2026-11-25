package com.waylays.chromeneon.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.cyberware.ClientCyberware;
import com.waylays.chromeneon.client.hud.draw.HudDraw;
import com.waylays.chromeneon.client.hud.theme.HudText;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.cyberware.BodySlot;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantState;
import com.waylays.chromeneon.common.cyberware.InstalledImplant;
import com.waylays.chromeneon.common.network.CyberwareRequestPayload;
import com.waylays.chromeneon.common.network.SyncCyberStatePayload;
import com.waylays.chromeneon.common.player.CyberStats;
import com.waylays.chromeneon.common.player.SyncedCyberState;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The cyberware interface (anywhere, key K): the body as a wireframe with each slot's node joined by a leader line
 * to its sockets, the humanity, capacity and RAM gauges, and the technical sheet of the selected implant with its
 * live state and its switch. Installation and removal happen in {@link RipperdocScreen}, at a ripperdoc chair.
 */
public class CyberwareScreen extends Screen implements com.waylays.chromeneon.client.hud.HudVisibility.FullScreenInterface {
    /** Width of a slot's label column. */
    private static final float COL = 96F;
    private static final ResourceLocation BODY = ChromeNeon.id("textures/gui/body.png");
    /** Node of each region on the body image (fractions of its width and height) and the side of its label. */
    private static final Map<BodySlot.Region, float[]> ANCHORS = Map.of(
            BodySlot.Region.HEAD, new float[] {0.57F, 0.05F, 1},
            BodySlot.Region.EYES, new float[] {0.45F, 0.075F, -1},
            BodySlot.Region.SPINE, new float[] {0.5F, 0.36F, 1},
            BodySlot.Region.HEART, new float[] {0.58F, 0.26F, 1},
            BodySlot.Region.SKIN, new float[] {0.38F, 0.3F, -1},
            BodySlot.Region.RIGHT_ARM, new float[] {0.23F, 0.44F, -1},
            BodySlot.Region.LEFT_ARM, new float[] {0.77F, 0.44F, 1},
            BodySlot.Region.LEGS, new float[] {0.43F, 0.66F, -1});

    protected record Socket(ResourceLocation slot, int index) {}

    private record Hit(float x0, float y0, float x1, float y1, Runnable action) {}

    protected Socket selected;
    private Component status;
    private boolean statusOk;
    private long statusAt;
    private final List<Hit> hits = new ArrayList<>();
    protected final List<NeonButton> buttons = new ArrayList<>();
    private Hit hovered;
    private final long openedAt = Util.getMillis();
    protected float px = 0.25F;

    public CyberwareScreen() {
        super(Component.translatable("chromeneon.screen.cyberware"));
    }

    protected CyberwareScreen(Component title) {
        super(title);
    }

    /** The server's answer to the last request. */
    public void status(Component message, boolean ok) {
        status = message;
        statusOk = ok;
        statusAt = Util.getMillis();
        play(ok ? ModSounds.UI_CLICK.get() : ModSounds.UI_ERROR.get());
    }

    protected static void play(net.minecraft.sounds.SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1F, 0.6F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected String header() {
        return "BODY INTERFACE";
    }

    protected float open() {
        return Math.min(1F, (Util.getMillis() - openedAt) / 350F);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        HudDraw.rect(g, 0, 0, width, height, NeonPalette.alpha(NeonPalette.VOID, 0.82F));
        for (float x = 0; x < width; x += 24F) HudDraw.rect(g, x, 0, x + px, height, NeonPalette.alpha(NeonPalette.VIOLET, 0.05F));
        for (float y = 0; y < height; y += 24F) HudDraw.rect(g, 0, y, width, y + px, NeonPalette.alpha(NeonPalette.VIOLET, 0.05F));
        float sweep = ((Util.getMillis() - openedAt) % 4000L) / 4000F * height;
        HudDraw.vGradient(g, 0, sweep - 30F, width, sweep, 0, NeonPalette.alpha(NeonPalette.CYAN, 0.05F));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        px = (float) (1.0 / Minecraft.getInstance().getWindow().getGuiScale());
        com.waylays.chromeneon.client.hud.theme.HudText.setScale(Minecraft.getInstance().getWindow().getGuiScale());
        renderBackground(g, mouseX, mouseY, partial);
        hits.clear();
        buttons.clear();
        SyncCyberStatePayload state = SyncedCyberState.current();
        float k = open();
        float m = 14F;
        HudText.draw(g, HudText.label("CHROME NÉON // CYBERWARE"), m, m, NeonPalette.alpha(NeonPalette.CYAN_SOFT, k), px);
        HudText.draw(g, HudText.title(header()), m, m + 7F, NeonPalette.alpha(NeonPalette.WHITE, k), px);
        HudDraw.rect(g, m, m + 17F, m + 120F * k, m + 17F + px, NeonPalette.alpha(NeonPalette.VIOLET, 0.8F));
        if (state == null) {
            HudText.draw(g, HudText.label("NO LINK"), m, m + 24F, NeonPalette.RED, px);
            return;
        }
        gauges(g, state.stats(), k);
        body(g, state, k, mouseX, mouseY);
        panel(g, state, mouseX, mouseY);
        extra(g, state, mouseX, mouseY);
        for (NeonButton b : buttons) b.draw(g, mouseX, mouseY, px);
        if (status != null && Util.getMillis() - statusAt < 6000L) {
            Component s = HudText.data(status);
            HudText.draw(g, s, m, height - m - 6F, statusOk ? NeonPalette.CYAN : NeonPalette.RED, px);
        }
        Hit hover = hits.stream().filter(h -> mouseX >= h.x0 && mouseX <= h.x1 && mouseY >= h.y0 && mouseY <= h.y1).findFirst().orElse(null);
        if (hover != null && hover != hovered && hovered != null) play(ModSounds.UI_HOVER.get());
        hovered = hover;
    }

    /** Humanity, capacity and RAM, top right. */
    private void gauges(GuiGraphics g, CyberStats s, float k) {
        float w = 130F, x = width - 14F - w, y = 14F;
        gauge(g, x, y, w, Component.translatable("chromeneon.screen.humanity"), s.humanity(), s.maxHumanity(),
                s.humanityRatio() < 0.25F ? NeonPalette.RED : NeonPalette.VIOLET_HOT, k);
        gauge(g, x, y + 14F, w, Component.translatable("chromeneon.screen.capacity"), s.capacity() - s.capacityUsed(), s.capacity(),
                NeonPalette.CYAN, k);
        gauge(g, x, y + 28F, w, Component.literal("RAM"), s.ram(), s.ramCapacity(), NeonPalette.CYAN_SOFT, k);
    }

    private void gauge(GuiGraphics g, float x, float y, float w, Component label, int value, int max, int color, float k) {
        HudText.draw(g, HudText.label(label), x, y, NeonPalette.alpha(NeonPalette.LAVENDER, k), px);
        Component v = HudText.data(value + " / " + max);
        HudText.drawRight(g, v, x + w, y - 0.5F, NeonPalette.alpha(color, k), px);
        int segs = Math.max(1, Math.min(40, max));
        float sw = w / segs;
        for (int i = 0; i < segs; i++) {
            boolean on = (i + 0.5F) / segs <= value / (float) Math.max(1, max) * k;
            HudDraw.parallelogram(g, x + i * sw, y + 6F, sw - Math.max(px, sw * 0.25F), 3F, 1F, NeonPalette.alpha(color, on ? 0.95F : 0.15F));
        }
    }

    /** The body, its slots, their leader lines and sockets. */
    private void body(GuiGraphics g, SyncCyberStatePayload state, float k, int mx, int my) {
        float bh = Math.min(height - 60F, 250F), bw = bh / 2F;
        float bx = width * 0.36F - bw / 2F, by = 36F + (height - 60F - bh) / 2F;
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Minecraft.getInstance().getTextureManager().getTexture(BODY).setFilter(true, false);
        g.setColor(NeonPalette.red(NeonPalette.VIOLET_HOT), NeonPalette.green(NeonPalette.VIOLET_HOT), NeonPalette.blue(NeonPalette.VIOLET_HOT), 0.7F * k);
        g.pose().pushPose();
        g.pose().translate(bx, by, 0);
        g.pose().scale(bw / 256F, bh / 512F, 1F);
        g.blit(BODY, 0, 0, 0, 0, 256, 512, 256, 512);
        g.pose().popPose();
        g.setColor(1F, 1F, 1F, 1F);

        float leftCol = bx - COL - 10F, rightCol = bx + bw + 16F;
        float[] nextY = {by - 2F, by - 2F};
        List<Map.Entry<net.minecraft.resources.ResourceKey<BodySlot>, BodySlot>> slots = new ArrayList<>(ModBodySlots.REGISTRY.entrySet());
        slots.sort((a, b) -> Float.compare(ANCHORS.getOrDefault(a.getValue().region(), new float[] {0, 0.5F, 1})[1],
                ANCHORS.getOrDefault(b.getValue().region(), new float[] {0, 0.5F, 1})[1]));
        for (var e : slots) {
            BodySlot slot = e.getValue();
            ResourceLocation id = e.getKey().location();
            float[] a = ANCHORS.getOrDefault(slot.region(), new float[] {0.5F, 0.5F, 1});
            float nx = bx + a[0] * bw, ny = by + a[1] * bh;
            boolean left = a[2] < 0;
            int side = left ? 0 : 1;
            float ly = Math.max(nextY[side], ny - 4F);
            float lx = left ? leftCol : rightCol;
            float blockH = 8F + slot.sockets() * 9F;
            nextY[side] = ly + blockH + 3F;
            boolean highlight = highlightSlot(id);
            int color = highlight ? NeonPalette.CYAN : NeonPalette.VIOLET_HOT;
            float pulse = highlight ? 0.6F + 0.4F * (float) Math.sin(Util.getMillis() / 150.0) : 1F;
            // Leader: node on the body, a diagonal, then along the label
            float joinX = left ? lx + COL : lx - 2F;
            HudDraw.line(g, nx, ny, joinX, ly + 3F, px, NeonPalette.alpha(color, 0.6F * k));
            HudDraw.rect(g, Math.min(joinX, left ? lx : lx + COL), ly + 3F, Math.max(joinX, left ? lx : lx + COL), ly + 3F + px,
                    NeonPalette.alpha(color, 0.5F * k));
            HudDraw.diamond(g, nx, ny, 2.2F, NeonPalette.alpha(NeonPalette.VOID, 0.8F * k));
            HudDraw.diamond(g, nx, ny, 1.6F, NeonPalette.alpha(color, pulse * k));
            HudText.draw(g, HudText.label(Component.translatable(BodySlot.translationKey(id))), lx, ly - 3.5F,
                    NeonPalette.alpha(NeonPalette.CYAN_SOFT, k), px);
            List<InstalledImplant> in = state.loadout().in(id);
            for (int i = 0; i < slot.sockets(); i++) {
                float ry = ly + 6F + i * 9F;
                Socket socket = new Socket(id, i);
                boolean sel = socket.equals(selected);
                boolean hover = mx >= lx && mx <= lx + COL && my >= ry && my <= ry + 8F;
                if (sel || hover) HudDraw.veil(g, lx - 2F, ry - 1F, lx + COL + 2F, ry + 8F, 2F, NeonPalette.alpha(color, sel ? 0.3F : 0.15F), 0);
                if (sel) HudDraw.rect(g, lx - 2F, ry - 1F, lx - 2F + px * 2, ry + 8F, NeonPalette.alpha(color, 1F));
                if (i < in.size()) {
                    InstalledImplant implant = in.get(i);
                    float chip = stateChip(g, implant.state(), lx + COL, ry + 1F, k);
                    String name = Component.translatable("chromeneon.implant." + implant.id().getPath()).getString();
                    HudText.draw(g, HudText.fit(HudText.Kind.BODY, name, COL - 6F - chip), lx + 2F, ry, NeonPalette.alpha(NeonPalette.WHITE, k), px);
                } else {
                    HudText.draw(g, HudText.label(Component.translatable("chromeneon.screen.empty_socket")), lx + 2F, ry + 1F,
                            NeonPalette.alpha(highlight ? NeonPalette.CYAN : NeonPalette.LAVENDER, (highlight ? pulse : 0.5F) * k), px);
                }
                hits.add(new Hit(lx - 2F, ry - 1F, lx + COL + 2F, ry + 8F, () -> {
                    selected = socket;
                    play(ModSounds.UI_CLICK.get());
                }));
            }
        }
    }

    /** Left edge of the sheet on the right: after the body and its right column, at 64% of the width at least. */
    protected float panelX() {
        float bw = Math.min(height - 60F, 250F) / 2F;
        return Math.max(width * 0.64F, width * 0.36F + bw / 2F + 16F + COL + 8F);
    }

    /** Slots to make pulse (the ripperdoc highlights those that accept the chosen implant). */
    protected boolean highlightSlot(ResourceLocation slot) {
        return false;
    }

    /** Draws the state right-aligned; returns its width. */
    private float stateChip(GuiGraphics g, ImplantState s, float right, float y, float k) {
        long now = ClientCyberware.now();
        String key;
        int color;
        if (s.malfunctioning(now)) {
            key = "malfunction";
            color = NeonPalette.RED;
        } else if (!s.enabled) {
            key = "off";
            color = NeonPalette.LAVENDER;
        } else if (s.active) {
            key = "active";
            color = NeonPalette.CYAN;
        } else if (s.coolingDown(now)) {
            key = "cooldown";
            color = NeonPalette.ORANGE;
        } else {
            key = "online";
            color = NeonPalette.VIOLET_HOT;
        }
        Component t = HudText.label(Component.translatable("chromeneon.screen.state." + key));
        HudText.drawRight(g, t, right, y, NeonPalette.alpha(color, k), px);
        return HudText.width(t);
    }

    /** The technical sheet of the selected socket. */
    private void panel(GuiGraphics g, SyncCyberStatePayload state, int mx, int my) {
        float x = panelX(), w = width - 14F - x, y = 64F;
        if (selected == null) {
            HudText.draw(g, HudText.label(Component.translatable("chromeneon.screen.select")), x, y, NeonPalette.LAVENDER, px);
            return;
        }
        var installed = state.loadout().get(selected.slot(), selected.index());
        if (installed.isEmpty()) {
            emptySocket(g, state, x, y, w);
            return;
        }
        Implant implant = ModImplants.get(installed.get().id()).orElse(null);
        if (implant == null) return;
        ImplantState s = installed.get().state();
        y = sheet(g, implant, installed.get().id(), x, y, w);
        y = live(g, implant, s, x, y + 2F, w);
        Component toggle = Component.translatable(s.enabled ? "chromeneon.screen.disable" : "chromeneon.screen.enable");
        Socket at = selected;
        buttons.add(new NeonButton(x, y + 4F, 58F, 9F, toggle, s.enabled ? NeonPalette.LAVENDER : NeonPalette.CYAN, true,
                () -> request(CyberwareRequestPayload.Action.SET_ENABLED, at, -1, !s.enabled)));
        actions(g, at, x + 66F, y + 4F, true);
    }

    /** What an empty socket offers (the ripperdoc adds the installation). */
    protected void emptySocket(GuiGraphics g, SyncCyberStatePayload state, float x, float y, float w) {
        HudText.draw(g, HudText.title(Component.translatable(BodySlot.translationKey(selected.slot()))), x, y, NeonPalette.WHITE, px);
        HudText.draw(g, HudText.label(Component.translatable("chromeneon.screen.empty_socket")), x, y + 10F, NeonPalette.LAVENDER, px);
        HudText.draw(g, HudText.label(Component.translatable("chromeneon.screen.need_ripperdoc")), x, y + 18F, NeonPalette.CYAN_SOFT, px);
    }

    /** More buttons for the selected installed implant (the ripperdoc adds removal). */
    protected void actions(GuiGraphics g, Socket socket, float x, float y, boolean installed) {}

    /** Extra panels (the ripperdoc adds the implants carried). */
    protected void extra(GuiGraphics g, SyncCyberStatePayload state, int mx, int my) {}

    /** Name, slot, mode, costs and description lines. Returns the y under it. */
    protected float sheet(GuiGraphics g, Implant implant, ResourceLocation id, float x, float y, float w) {
        HudText.draw(g, HudText.title(Component.translatable("chromeneon.implant." + id.getPath())), x, y, NeonPalette.WHITE, px);
        HudDraw.rect(g, x, y + 9F, x + w, y + 9F + px, NeonPalette.alpha(NeonPalette.VIOLET, 0.7F));
        Implant.Properties p = implant.properties();
        HudText.draw(g, HudText.label(Component.translatable("chromeneon.tooltip.mode." + p.mode().name().toLowerCase(Locale.ROOT))),
                x, y + 12F, NeonPalette.CYAN_SOFT, px);
        Component costs = HudText.data(Component.translatable("chromeneon.screen.costs", p.humanityCost(), p.capacityCost()));
        HudText.drawRight(g, costs, x + w, y + 12F, NeonPalette.VIOLET_HOT, px);
        float ly = y + 21F;
        for (Component line : implant.describe()) {
            for (FormattedCharSequence seq : font.split(HudText.of(HudText.Kind.BODY, line), (int) w)) {
                g.pose().pushPose();
                g.pose().translate(x, ly, 0);
                g.drawString(font, seq, 0, 0, NeonPalette.LAVENDER, false);
                g.pose().popPose();
                ly += 7F;
            }
            ly += 1.5F;
        }
        return ly;
    }

    /** The implant's live state: timers, charges, gauges. Returns the y under it. */
    private float live(GuiGraphics g, Implant implant, ImplantState s, float x, float y, float w) {
        long now = ClientCyberware.now();
        List<String> lines = new ArrayList<>();
        if (s.malfunctioning(now)) lines.add(String.format(Locale.ROOT, "MALFUNCTION %.1fS", (s.malfunctionUntil - now) / 20F));
        if (s.active && s.activeUntil > now) lines.add(String.format(Locale.ROOT, "ACTIVE %.1fS", (s.activeUntil - now) / 20F));
        if (s.coolingDown(now)) lines.add(String.format(Locale.ROOT, "COOLDOWN %.1fS", (s.cooldownUntil - now) / 20F));
        if (implant == ModImplants.HEMOSTAT_PUMP.get() || implant == ModImplants.SPRING_TENDONS.get()) lines.add("CHARGES " + s.charges);
        if (implant == ModImplants.CARAPACE_DERMIS.get()) lines.add(String.format(Locale.ROOT, "INTEGRITY %.1f", s.resource));
        for (String line : lines) {
            HudText.draw(g, HudText.data(line), x, y, NeonPalette.CYAN, px);
            y += 7F;
        }
        return y;
    }

    protected void request(CyberwareRequestPayload.Action action, Socket socket, int inventorySlot, boolean flag) {
        PacketDistributor.sendToServer(new CyberwareRequestPayload(action, socket.slot(), socket.index(), inventorySlot, flag));
        play(ModSounds.UI_CLICK.get());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (NeonButton b : buttons) {
            if (b.enabled() && b.contains(mx, my)) {
                b.action().run();
                return true;
            }
        }
        for (Hit h : hits) {
            if (mx >= h.x0 && mx <= h.x1 && my >= h.y0 && my <= h.y1) {
                h.action.run();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** For the automated test: selects a socket as a click would. */
    public void select(ResourceLocation slot, int index) {
        selected = new Socket(slot, index);
    }
}
