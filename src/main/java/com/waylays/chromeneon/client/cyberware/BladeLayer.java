package com.waylays.chromeneon.client.cyberware;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Third person: the blades on the player model's arms, following every arm movement and swing. */
public final class BladeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public BladeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        float delta = net.minecraft.client.Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
        for (boolean left : new boolean[] {false, true}) {
            float ext = BladeAnimation.extension(player.getId(), left, BladeRenderer.deployed(player, left), delta);
            if (ext <= 0.01F) continue;
            pose.pushPose();
            (left ? getParentModel().leftArm : getParentModel().rightArm).translateAndRotate(pose);
            BladeRenderer.render(pose, buffers, light, ext, left);
            pose.popPose();
        }
    }
}
