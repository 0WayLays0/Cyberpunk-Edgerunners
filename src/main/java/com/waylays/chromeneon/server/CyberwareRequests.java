package com.waylays.chromeneon.server;

import com.waylays.chromeneon.common.block.RipperdocChairBlock;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.CyberwareService;
import com.waylays.chromeneon.common.cyberware.InstallResult;
import com.waylays.chromeneon.common.cyberware.UseResult;
import com.waylays.chromeneon.common.cyberware.mechanics.AirJump;
import com.waylays.chromeneon.common.cyberware.mechanics.BladeCombat;
import com.waylays.chromeneon.common.network.ActionFeedbackPayload;
import com.waylays.chromeneon.common.network.CyberwareRequestPayload;
import com.waylays.chromeneon.common.network.ImplantTriggerPayload;
import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles what clients ask about cyberware, on the server thread. Every request is checked by the service; a client
 * flooding requests is cut off for the rest of the tick. Each outcome is reported back so the interfaces can say
 * why something was refused.
 */
public final class CyberwareRequests {
    private static final int PER_TICK = 8;
    private static final Map<UUID, long[]> RATE = new HashMap<>();

    private CyberwareRequests() {}

    private static boolean allowed(ServerPlayer player) {
        long now = player.level().getGameTime();
        long[] r = RATE.computeIfAbsent(player.getUUID(), k -> new long[] {now, 0});
        if (r[0] != now) {
            r[0] = now;
            r[1] = 0;
        }
        return ++r[1] <= PER_TICK && player.isAlive();
    }

    public static void handle(ServerPlayer player, CyberwareRequestPayload request) {
        if (!allowed(player)) return;
        boolean seated = RipperdocChairBlock.seatedIn(player) != null;
        InstallResult result = switch (request.action()) {
            case INSTALL -> CyberwareService.install(player, seated, request.slot(), request.inventorySlot());
            case REMOVE -> CyberwareService.remove(player, seated, request.slot(), request.index());
            case SET_ENABLED -> CyberwareService.setEnabled(player, request.slot(), request.index(), request.flag());
        };
        feedback(player, result.translationKey(), result.ok(), 0);
        ImplantVisuals.update(player);
    }

    public static void handle(ServerPlayer player, ImplantTriggerPayload trigger) {
        if (!allowed(player)) return;
        UseResult result = switch (trigger.trigger()) {
            case ACCELERATOR -> CyberwareService.use(player, ModImplants.CHRONOS_ACCELERATOR.get());
            case BLADES -> CyberwareService.useAll(player, ModImplants.REAPER_BLADES.get());
            case LUNGE -> BladeCombat.lunge(player, CyberData.get(player), trigger.target());
            case AIR_JUMP -> AirJump.jump(player);
            case OPTICS_POWER -> CyberwareService.use(player, ModImplants.ARGUS_OPTICS.get());
            case ANALYSIS -> CyberwareService.analyse(player);
        };
        CyberData.sync(player);
        ImplantVisuals.update(player);
        // A refused air jump is silent (the jump key is pressed all the time); everything else is reported
        if (result.ok() || trigger.trigger() == ImplantTriggerPayload.Trigger.AIR_JUMP) return;
        int ram = result == UseResult.NO_RAM ? switch (trigger.trigger()) {
            case ACCELERATOR -> ServerConfig.ACCEL_RAM.get();
            case ANALYSIS -> ServerConfig.TACTICAL_RAM.get();
            default -> 0;
        } : 0;
        feedback(player, result.translationKey(), false, ram);
    }

    static void feedback(ServerPlayer player, String key, boolean ok, int ram) {
        if (player.connection != null && player.connection.hasChannel(ActionFeedbackPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new ActionFeedbackPayload(key, ok, ram));
        }
    }

    static void forget(ServerPlayer player) {
        RATE.remove(player.getUUID());
    }
}
