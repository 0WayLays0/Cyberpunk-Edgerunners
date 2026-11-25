package com.waylays.chromeneon.client.cyberware;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.network.ImplantVisualsPayload;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Draws a blade in arm space (after the arm's model part has placed the pose): a flat steel blade along the outer
 * side of the forearm, reaching past the hand as it extends, with a glowing violet edge. The same geometry serves
 * the first-person arm and the third-person player model.
 */
public final class BladeRenderer {
    private static final ResourceLocation TEXTURE = ChromeNeon.id("textures/entity/reaper_blade.png");
    private static final float P = 1F / 16F;
    /** Degrees the blade turns around the forearm. */
    private static final float TWIST = 35F;

    private BladeRenderer() {}

    /** Whether a player's blade is out on that arm (this client's own data, or what the server shows of others). */
    public static boolean deployed(Player player, boolean left) {
        if (player == Minecraft.getInstance().player) {
            return ClientCyberware.of(ModImplants.REAPER_BLADES.get()).stream()
                    .anyMatch(e -> e.slot().equals(left ? ModBodySlots.LEFT_ARM : ModBodySlots.RIGHT_ARM)
                            && ClientCyberware.working(e) && e.installed().state().active);
        }
        int flags = ClientCyberware.visuals(player.getId());
        return (flags & (left ? ImplantVisualsPayload.LEFT_BLADE : ImplantVisualsPayload.RIGHT_BLADE)) != 0;
    }

    /**
     * @param extension 0 (stowed) to 1 (fully out)
     * @param left      left arm (the blade sits on the mirrored side)
     */
    public static void render(PoseStack pose, MultiBufferSource buffers, int light, float extension, boolean left) {
        if (extension <= 0.01F) return;
        float side = left ? 1F : -1F;
        float x0 = side * 3.05F * P, x1 = side * 3.45F * P;
        float len = 15F * extension;
        // Blade outline in (y, z), pixels: base on the forearm, belly, tip beyond the hand, spine back
        float[][] shape = {{3.5F, 1.4F}, {3.5F, -1.2F}, {9F + len * 0.55F, -1.9F}, {9F + len, 0.1F}, {9F + len * 0.5F, 1.3F}};
        // Twisted a little around the forearm, so the flat of the blade shows from the front as well as from the side
        pose.pushPose();
        pose.translate(side * 3.25F * P, 0F, 0F);
        pose.mulPose(Axis.YP.rotationDegrees(side * TWIST));
        pose.translate(-side * 3.25F * P, 0F, 0F);
        PoseStack.Pose last = pose.last();
        VertexConsumer steel = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        face(steel, last, shape, x1, side, light);
        face(steel, last, shape, x0, -side, light);
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        int edge = NeonPalette.alpha(NeonPalette.VIOLET_HOT, 0.9F * extension);
        float xe = side * 3.6F * P;
        // The cutting edge: belly to tip, drawn as a thin glowing ribbon just outside the blade
        edgeLine(glow, last, xe, shape[1], shape[2], edge);
        edgeLine(glow, last, xe, shape[2], shape[3], edge);
        pose.popPose();
    }

    private static void face(VertexConsumer vc, PoseStack.Pose pose, float[][] s, float x, float normal, int light) {
        // Fan from the first point: (0,1,2,3) then (0,3,4,4)
        int[][] quads = {{0, 1, 2, 3}, {0, 3, 4, 4}};
        for (int[] q : quads) {
            int[] order = normal > 0 ? q : new int[] {q[3], q[2], q[1], q[0]};
            for (int i : order) {
                float y = s[i][0] * P, z = s[i][1] * P;
                float u = Math.clamp((s[i][1] + 2F) / 4F, 0F, 1F), v = Math.clamp((s[i][0] - 3.5F) / 21F, 0F, 1F);
                vc.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(pose, normal, 0F, 0F);
            }
        }
    }

    private static void edgeLine(VertexConsumer vc, PoseStack.Pose pose, float x, float[] a, float[] b, int color) {
        float[][] q = {{a[0], a[1] - 0.35F}, {b[0], b[1] - 0.35F}, {b[0], b[1] + 0.35F}, {a[0], a[1] + 0.35F}};
        for (int i = 0; i < 4; i++) vc.addVertex(pose, x, q[i][0] * P, q[i][1] * P).setColor(color);
        for (int i = 3; i >= 0; i--) vc.addVertex(pose, x, q[i][0] * P, q[i][1] * P).setColor(color);
    }
}
