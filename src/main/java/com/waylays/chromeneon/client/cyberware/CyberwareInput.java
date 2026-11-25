package com.waylays.chromeneon.client.cyberware;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.input.ChromeKeys;
import com.waylays.chromeneon.client.screen.CyberwareScreen;
import com.waylays.chromeneon.common.cyberware.ImplantState;
import com.waylays.chromeneon.common.cyberware.mechanics.CyberMath;
import com.waylays.chromeneon.common.network.ImplantTriggerPayload;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.config.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The player's hands on their cyberware: the implant keys, the extra jump (jump pressed again in mid-air with the
 * tendons), and the lunge (attacking the air while sprinting with the blades out). The client only asks; the server
 * decides. The extra jump moves the player at once because movement is the client's in Minecraft.
 */
@EventBusSubscriber(modid = ChromeNeon.MODID, value = Dist.CLIENT)
public final class CyberwareInput {
    private static boolean jumpWasDown;
    private static boolean leftGroundSinceJump;
    private static int jumpsLeft;

    private CyberwareInput() {}

    static void send(ImplantTriggerPayload.Trigger trigger, int target) {
        PacketDistributor.sendToServer(new ImplantTriggerPayload(trigger, target));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) return;
        while (ChromeKeys.ACCELERATOR.consumeClick()) send(ImplantTriggerPayload.Trigger.ACCELERATOR, -1);
        while (ChromeKeys.BLADES.consumeClick()) send(ImplantTriggerPayload.Trigger.BLADES, -1);
        while (ChromeKeys.ANALYSIS.consumeClick()) send(ImplantTriggerPayload.Trigger.ANALYSIS, -1);
        while (ChromeKeys.CYBERWARE.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new CyberwareScreen());
        }
        airJump(mc, player);
    }

    /** A second press of jump in mid-air, with working tendons and a jump left, pushes the player up. */
    private static void airJump(Minecraft mc, LocalPlayer player) {
        boolean down = mc.options.keyJump.isDown();
        if (player.onGround() || player.isInLiquid() || player.onClimbable()) {
            leftGroundSinceJump = false;
            jumpsLeft = ClientCyberware.state(ModImplants.SPRING_TENDONS.get()).map(s -> s.charges).orElse(0);
        } else if (!down) {
            leftGroundSinceJump = true;
        }
        boolean pressed = down && !jumpWasDown;
        jumpWasDown = down;
        if (!pressed || !leftGroundSinceJump) return;
        var tendons = ClientCyberware.first(ModImplants.SPRING_TENDONS.get());
        if (tendons.isEmpty() || !ClientCyberware.working(tendons.get())) return;
        boolean flying = player.getAbilities().flying || player.isFallFlying();
        if (!CyberMath.canAirJump(false, flying, player.isInLiquid(), player.isPassenger(), jumpsLeft)) return;
        jumpsLeft--;
        Vec3 v = player.getDeltaMovement();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize().scale(0.15);
        player.setDeltaMovement(v.x + look.x, ServerConfig.JUMP_VELOCITY.get(), v.z + look.z);
        player.resetFallDistance();
        ChromeHud.ping(player.position(), 1.5F, com.waylays.chromeneon.client.hud.theme.NeonPalette.CYAN);
        send(ImplantTriggerPayload.Trigger.AIR_JUMP, -1);
    }

    /** Attacking the air while sprinting with the blades out lunges, toward the optics' target if any. */
    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!event.isAttack() || player == null || !player.getMainHandItem().isEmpty() || !player.isSprinting()) return;
        if (!ClientCyberware.activeAndWorking(ModImplants.REAPER_BLADES.get())) return;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) return;
        ImplantState blades = ClientCyberware.state(ModImplants.REAPER_BLADES.get()).orElse(null);
        if (blades != null && blades.coolingDown(ClientCyberware.now())) return;
        Entity target = ClientCyberware.opticsOnline() ? targetedEntity() : null;
        send(ImplantTriggerPayload.Trigger.LUNGE, target == null ? -1 : target.getId());
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    private static Entity targetedEntity() {
        var subject = ChromeHud.target().subject();
        return subject instanceof com.waylays.chromeneon.client.hud.scan.ScanSubject.EntitySubject e ? e.entity() : null;
    }
}
