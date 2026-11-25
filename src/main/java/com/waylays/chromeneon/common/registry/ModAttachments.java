package com.waylays.chromeneon.common.registry;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.player.PlayerCyberData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ChromeNeon.MODID);

    /** Saved with the player; cyberware survives death (it is part of the body, not of the inventory). */
    public static final Supplier<AttachmentType<PlayerCyberData>> CYBER_DATA = ATTACHMENTS.register("cyber_data",
            () -> AttachmentType.builder(PlayerCyberData::new)
                    .serialize(PlayerCyberData.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
