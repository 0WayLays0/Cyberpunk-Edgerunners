package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's sounds, all original (synthesised by {@code tools/sounds.py}). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ChromeNeon.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> IMPLANT_INSTALL = sound("implant.install");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPLANT_REMOVE = sound("implant.remove");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATOR_ON = sound("accelerator.on");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATOR_OFF = sound("accelerator.off");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLADES_DEPLOY = sound("blades.deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLADES_RETRACT = sound("blades.retract");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLADES_HIT = sound("blades.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLADES_LUNGE = sound("blades.lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> TENDONS_JUMP = sound("tendons.jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> DERMIS_ABSORB = sound("dermis.absorb");
    public static final DeferredHolder<SoundEvent, SoundEvent> DERMIS_BREACH = sound("dermis.breach");
    public static final DeferredHolder<SoundEvent, SoundEvent> PUMP_INJECT = sound("pump.inject");
    public static final DeferredHolder<SoundEvent, SoundEvent> OPTICS_SCAN = sound("optics.scan");
    public static final DeferredHolder<SoundEvent, SoundEvent> OPTICS_ZOOM = sound("optics.zoom");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYSTEM_ON = sound("system.on");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYSTEM_OFF = sound("system.off");
    public static final DeferredHolder<SoundEvent, SoundEvent> MALFUNCTION = sound("malfunction");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAIR_SIT = sound("chair.sit");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_HOVER = sound("ui.hover");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLICK = sound("ui.click");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_ERROR = sound("ui.error");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ChromeNeon.id(name)));
    }
}
