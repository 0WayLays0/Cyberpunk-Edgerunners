package com.waylays.chromeneon.client.cyberware;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.block.RipperdocChairBlock;
import com.waylays.chromeneon.common.block.RipperdocChairBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

/**
 * The chair's hologram: idle, a slow ring turns above the headrest; with a patient seated, a scanning ring sweeps
 * up and down around them with a second ring turning the other way.
 */
public final class ChairRenderer implements BlockEntityRenderer<RipperdocChairBlockEntity> {
    public ChairRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RipperdocChairBlockEntity chair, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (chair.getLevel() == null) return;
        float t = chair.getLevel().getGameTime() + partial;
        boolean occupied = RipperdocChairBlock.occupied(chair.getLevel(), chair.getBlockPos());
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        Matrix4f m = pose.last().pose();
        if (occupied) {
            float y = 0.5F + 0.75F * (0.5F + 0.5F * (float) Math.sin(t * 0.08F));
            ring(vc, m, y, 0.62F, 0.025F, t * 0.05F, 3, 0.55F, NeonPalette.alpha(NeonPalette.CYAN, 0.75F));
            ring(vc, m, y + 0.05F, 0.66F, 0.012F, -t * 0.08F, 12, 0.4F, NeonPalette.alpha(NeonPalette.VIOLET_HOT, 0.6F));
        } else {
            ring(vc, m, 1.35F, 0.32F, 0.015F, t * 0.03F, 6, 0.5F, NeonPalette.alpha(NeonPalette.VIOLET_HOT, 0.45F));
        }
        pose.popPose();
    }

    /** A horizontal ring of {@code dashes} arcs at height {@code y}. */
    private static void ring(VertexConsumer vc, Matrix4f m, float y, float r, float h, float phase, int dashes, float fill, int color) {
        int steps = 64;
        float dash = (float) (Math.PI * 2 / dashes);
        for (int i = 0; i < steps; i++) {
            float a0 = phase + (float) (Math.PI * 2) * i / steps, a1 = phase + (float) (Math.PI * 2) * (i + 1) / steps;
            if (((a0 - phase) % dash) / dash > fill) continue;
            float x0 = (float) Math.cos(a0) * r, z0 = (float) Math.sin(a0) * r, x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r;
            // Both windings: the ring is seen from inside and outside
            vc.addVertex(m, x0, y - h, z0).setColor(color);
            vc.addVertex(m, x1, y - h, z1).setColor(color);
            vc.addVertex(m, x1, y + h, z1).setColor(color);
            vc.addVertex(m, x0, y + h, z0).setColor(color);
            vc.addVertex(m, x0, y + h, z0).setColor(color);
            vc.addVertex(m, x1, y + h, z1).setColor(color);
            vc.addVertex(m, x1, y - h, z1).setColor(color);
            vc.addVertex(m, x0, y - h, z0).setColor(color);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(RipperdocChairBlockEntity chair) {
        return true;
    }
}
