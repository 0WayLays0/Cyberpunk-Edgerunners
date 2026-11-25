package com.waylays.chromeneon.common.cyberware.implants;

import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Biochip implant: a coprocessor on the neural bus. It adds RAM cells and speeds up their recovery (the RAM pool
 * reads both while it works), so the RAM-hungry systems (accelerator, analysis, later the quickhacks) run more often,
 * and it shortens the operating system's cooldowns.
 */
public final class SynapseCoprocessor extends Implant {
    public SynapseCoprocessor() {
        super(Properties.of(Mode.PASSIVE, 8, 4, ModBodySlots.BIOCHIP).ram(2));
    }

    @Override
    public List<Component> describe() {
        return List.of(ImplantText.line("synapse_coprocessor", 0, ramBonus()),
                ImplantText.line("synapse_coprocessor", 1, Math.round((1.0 - ServerConfig.COPROCESSOR_REGEN.get()) * 100)),
                ImplantText.line("synapse_coprocessor", 2, Math.round(ServerConfig.COPROCESSOR_COOLDOWN.get() * 100)),
                ImplantText.line("synapse_coprocessor", 3));
    }

    @Override
    public double cooldownFactor(Implant other) {
        return other.operatingSystem() ? 1.0 - ServerConfig.COPROCESSOR_COOLDOWN.get() : 1.0;
    }
}
