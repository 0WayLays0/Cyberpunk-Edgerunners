package com.waylays.chromeneon.client.cyberware;

import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.screen.CyberwareScreen;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.network.ActionFeedbackPayload;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

/**
 * The server's answer to a request: shown in the cyberware interface when it is open, otherwise on the HUD. Short
 * RAM makes the missing cells flash.
 */
public final class CyberwareFeedback {
    private CyberwareFeedback() {}

    public static void accept(ActionFeedbackPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.ramNeeded() > 0) ChromeHud.ramInsufficient(payload.ramNeeded());
        if (mc.screen instanceof CyberwareScreen screen) {
            screen.status(Component.translatable(payload.key()), payload.ok());
            return;
        }
        if (payload.ok()) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.UI_ERROR.get(), 1F, 0.6F));
        ChromeHud.notify(payload.ramNeeded() > 0 ? NotificationKind.WARNING : NotificationKind.ERROR,
                Component.translatable("chromeneon.use.refused"), Component.translatable(payload.key()));
    }
}
