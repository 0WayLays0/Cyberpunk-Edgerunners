package com.waylays.chromeneon.common.cyberware.implants;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.UseResult;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * Arm implant: blades that fold out of the forearm. Deployed, they replace the empty hand in combat
 * ({@link com.waylays.chromeneon.common.cyberware.mechanics.BladeCombat}): stronger, faster strikes and a lunge.
 * One set per arm; two sets strike harder.
 */
public final class ReaperBlades extends Implant {
    public ReaperBlades() {
        super(Properties.of(Mode.TOGGLE, 12, 6, ModBodySlots.LEFT_ARM, ModBodySlots.RIGHT_ARM).perBody(2));
    }

    @Override
    public List<Component> describe() {
        return List.of(
                ImplantText.line("reaper_blades", 0, String.format(java.util.Locale.ROOT, "%.1f", ServerConfig.BLADE_DAMAGE.get())),
                ImplantText.line("reaper_blades", 1, String.format(java.util.Locale.ROOT, "%.1f", ServerConfig.LUNGE_DAMAGE.get()),
                        ImplantText.seconds(ServerConfig.LUNGE_COOLDOWN.get())),
                ImplantText.line("reaper_blades", 2),
                ImplantText.line("reaper_blades", 3, Math.round(ServerConfig.DILATION_BLADE_BONUS.get() * 100)));
    }

    @Override
    public UseResult use(ImplantContext ctx) {
        UseResult result = super.use(ctx);
        if (result.ok()) {
            ctx.player().level().playSound(null, ctx.player().blockPosition(),
                    (ctx.state().active ? ModSounds.BLADES_DEPLOY : ModSounds.BLADES_RETRACT).get(), SoundSource.PLAYERS, 0.9F, 1F);
        }
        return result;
    }
}
