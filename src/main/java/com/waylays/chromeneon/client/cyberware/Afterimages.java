package com.waylays.chromeneon.client.cyberware;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The accelerator's trail: while a player runs a time dilation, copies of their body stay behind them as glowing
 * holograms (violet to cyan, scanlined) and fade in a little over half a second. Every client draws them for every
 * dilating player it sees, from the poses it recorded; nothing goes over the network but the dilation itself.
 */
@EventBusSubscriber(modid = ChromeNeon.MODID, value = Dist.CLIENT)
public final class Afterimages {
    private static final ResourceLocation TEXTURE = ChromeNeon.id("textures/entity/afterimage.png");
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    /** Ticks a copy lasts, ticks between two copies, copies kept per player. */
    private static final int LIFE = 12, EVERY = 2, MAX = 7;
    private static final double MIN_MOVE_SQR = 0.3 * 0.3;
    private static final int[] COLORS = {NeonPalette.VIOLET_HOT, NeonPalette.VIOLET, 0xFF8A5CFF, NeonPalette.CYAN};

    /** A recorded pose. */
    private record Ghost(Vec3 pos, float bodyYaw, float headYaw, float pitch, float limbPos, float limbSpeed, float attack,
                         boolean crouching, float age, long born, int index) {}

    private static final Map<Integer, ArrayDeque<Ghost>> GHOSTS = new HashMap<>();
    private static long ticks;
    private static int counter;

    private Afterimages() {}

    private static boolean dilating(AbstractClientPlayer player) {
        var mc = Minecraft.getInstance();
        return ClientCyberware.slowdown(player.getId()) > 1 || (player == mc.player && ClientCyberware.dilating());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            GHOSTS.clear();
            return;
        }
        ticks++;
        for (AbstractClientPlayer player : level.players()) {
            if (!dilating(player) || player.isSpectator() || player.isInvisible()) continue;
            ArrayDeque<Ghost> trail = GHOSTS.computeIfAbsent(player.getId(), id -> new ArrayDeque<>());
            Ghost last = trail.peekLast();
            boolean moved = last == null || last.pos().distanceToSqr(player.position()) > MIN_MOVE_SQR;
            if (ticks % EVERY != 0 || !(moved || player.swinging)) continue;
            trail.addLast(new Ghost(player.position(), player.yBodyRot, player.yHeadRot, player.getXRot(),
                    player.walkAnimation.position(), Math.min(1F, player.walkAnimation.speed()), player.getAttackAnim(1F),
                    player.isCrouching(), player.tickCount, ticks, counter++));
            if (trail.size() > MAX) trail.removeFirst();
        }
        Iterator<Map.Entry<Integer, ArrayDeque<Ghost>>> it = GHOSTS.entrySet().iterator();
        while (it.hasNext()) {
            ArrayDeque<Ghost> trail = it.next().getValue();
            while (!trail.isEmpty() && ticks - trail.peekFirst().born() > LIFE) trail.removeFirst();
            if (trail.isEmpty()) it.remove();
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || GHOSTS.isEmpty()) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(TYPE);
        PoseStack pose = event.getPoseStack();
        GHOSTS.forEach((id, trail) -> {
            if (!(mc.level.getEntity(id) instanceof AbstractClientPlayer player)) return;
            if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) return;
            PlayerModel<AbstractClientPlayer> model = renderer.getModel();
            for (Ghost g : trail) {
                // In first person, the newest copies sit on the camera: skip what is too close to see
                if (firstPerson && player == mc.player && g.pos().distanceToSqr(cam) < 2.25) continue;
                float life = (ticks - g.born() + partial) / LIFE;
                if (life >= 1F) continue;
                float alpha = 0.55F * (float) Math.pow(1F - life, 1.4);
                draw(pose, consumer, model, player, g, cam, NeonPalette.fade(COLORS[g.index() % COLORS.length], alpha));
            }
        });
        buffers.endBatch(TYPE);
    }

    private static void draw(PoseStack pose, VertexConsumer consumer, PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player,
                             Ghost g, Vec3 cam, int color) {
        pose.pushPose();
        pose.translate(g.pos().x - cam.x, g.pos().y - cam.y + (g.crouching() ? -0.125 : 0), g.pos().z - cam.z);
        // The transforms of the living entity renderer, from the recorded pose
        pose.mulPose(Axis.YP.rotationDegrees(180F - g.bodyYaw()));
        pose.scale(-1F, -1F, 1F);
        pose.scale(0.9375F, 0.9375F, 0.9375F);
        pose.translate(0F, -1.501F, 0F);
        model.attackTime = g.attack();
        model.riding = false;
        model.young = false;
        model.crouching = g.crouching();
        model.setupAnim(player, g.limbPos(), g.limbSpeed(), g.age(), Mth.wrapDegrees(g.headYaw() - g.bodyYaw()), g.pitch());
        model.renderToBuffer(pose, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
    }

    public static void clear() {
        GHOSTS.clear();
    }
}
