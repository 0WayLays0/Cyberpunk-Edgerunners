package com.waylays.chromeneon.client.cyberware;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person: with a blade out on an empty hand, the arm is drawn as vanilla draws a bare arm (same transforms,
 * same swing) and the blade on it. The off arm is drawn too when its blade is out, which vanilla never does for an
 * empty off hand.
 */
@EventBusSubscriber(modid = ChromeNeon.MODID, value = Dist.CLIENT)
public final class BladeHandRender {
    private BladeHandRender() {}

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || !event.getItemStack().isEmpty() || player.isInvisible()) return;
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean left = arm == HumanoidArm.LEFT;
        float ext = BladeAnimation.extension(player.getId(), left, BladeRenderer.deployed(player, left), mc.getTimer().getRealtimeDeltaTicks());
        if (ext <= 0.01F) return;
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float swing = event.getHand() == InteractionHand.MAIN_HAND ? event.getSwingProgress() : 0F;
        armTransform(pose, event.getEquipProgress(), swing, arm);
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().<AbstractClientPlayer>getRenderer(player);
        if (left) renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        else renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        (left ? renderer.getModel().leftArm : renderer.getModel().rightArm).translateAndRotate(pose);
        BladeRenderer.render(pose, event.getMultiBufferSource(), event.getPackedLight(), ext, left);
        pose.popPose();
    }

    /** The transforms vanilla applies to a bare first-person arm (ItemInHandRenderer#renderPlayerArm). */
    private static void armTransform(PoseStack pose, float equip, float swing, HumanoidArm arm) {
        float f = arm != HumanoidArm.LEFT ? 1F : -1F;
        float s = Mth.sqrt(swing);
        float a = -0.3F * Mth.sin(s * (float) Math.PI);
        float b = 0.4F * Mth.sin(s * (float) (Math.PI * 2));
        float c = -0.4F * Mth.sin(swing * (float) Math.PI);
        pose.translate(f * (a + 0.64000005F), b - 0.6F + equip * -0.6F, c - 0.71999997F);
        pose.mulPose(Axis.YP.rotationDegrees(f * 45F));
        float d = Mth.sin(swing * swing * (float) Math.PI);
        float e = Mth.sin(s * (float) Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(f * e * 70F));
        pose.mulPose(Axis.ZP.rotationDegrees(f * d * -20F));
        pose.translate(f * -1F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(f * 120F));
        pose.mulPose(Axis.XP.rotationDegrees(200F));
        pose.mulPose(Axis.YP.rotationDegrees(f * -135F));
        pose.translate(f * 5.6F, 0F, 0F);
    }
}
