package com.waylays.chromeneon.common.cyberware;

import com.waylays.chromeneon.common.player.PlayerCyberData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Everything an implant hook needs: the player, their cyber data, where the implant sits, its state, the time. */
public record ImplantContext(ServerPlayer player, PlayerCyberData data, ResourceLocation slot, int index,
                             InstalledImplant installed, long now) {
    public ImplantState state() {
        return installed.state();
    }

    public boolean working() {
        return installed.state().working(now);
    }
}
