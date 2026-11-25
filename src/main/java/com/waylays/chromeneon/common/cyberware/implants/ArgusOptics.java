package com.waylays.chromeneon.common.cyberware.implants;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.UseResult;
import com.waylays.chromeneon.common.cyberware.mechanics.Cooldowns;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * Optic implant. Online, it gives the scanner and the zoom (both drawn by the client from the real targets), and
 * its target guides other implants (the blades' lunge). A tactical analysis, paid in RAM, marks every creature in
 * range for a while, even through walls; it runs for free while time is dilated.
 */
public final class ArgusOptics extends Implant {
    public ArgusOptics() {
        super(Properties.of(Mode.TOGGLE, 8, 4, ModBodySlots.OPTICS));
    }

    @Override
    public List<Component> describe() {
        return List.of(
                ImplantText.line("argus_optics", 0, String.format(java.util.Locale.ROOT, "%.0f", ServerConfig.OPTICS_MAX_ZOOM.get())),
                ImplantText.line("argus_optics", 1, ServerConfig.TACTICAL_RANGE.get(), ImplantText.seconds(ServerConfig.TACTICAL_DURATION.get()),
                        ServerConfig.TACTICAL_RAM.get()),
                ImplantText.line("argus_optics", 2));
    }

    @Override
    public void onInstalled(ImplantContext ctx) {
        ctx.state().setActive(true, ctx.now());
    }

    /** Whether the optics are online and working: what the scanner and the zoom require. */
    public static boolean online(ImplantContext ctx) {
        return ctx.working() && ctx.state().active;
    }

    /** Starts a tactical analysis. */
    public UseResult analyse(ImplantContext ctx) {
        UseResult ready = ready(ctx);
        if (!ready.ok()) return ready;
        if (!ctx.state().active) return UseResult.OFFLINE;
        if (ctx.state().coolingDown(ctx.now())) return UseResult.COOLDOWN;
        if (!ctx.data().ram().tryConsume(ServerConfig.TACTICAL_RAM.get())) return UseResult.NO_RAM;
        ctx.state().activeUntil = ctx.now() + ServerConfig.TACTICAL_DURATION.get();
        ctx.state().cooldownUntil = ctx.state().activeUntil + Cooldowns.of(ctx, this, ServerConfig.TACTICAL_COOLDOWN.get());
        ctx.player().level().playSound(null, ctx.player().blockPosition(), ModSounds.OPTICS_SCAN.get(), SoundSource.PLAYERS, 0.8F, 1F);
        return UseResult.OK;
    }

    @Override
    public UseResult use(ImplantContext ctx) {
        UseResult result = super.use(ctx);
        if (result.ok()) {
            ctx.player().level().playSound(null, ctx.player().blockPosition(),
                    (ctx.state().active ? ModSounds.SYSTEM_ON : ModSounds.SYSTEM_OFF).get(), SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return result;
    }

    @Override
    public void onStopped(ImplantContext ctx) {
        ctx.state().activeUntil = Math.min(ctx.state().activeUntil, ctx.now());
    }
}
