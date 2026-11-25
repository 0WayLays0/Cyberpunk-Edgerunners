package com.waylays.chromeneon.gametest;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.block.RipperdocChairBlock;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.hud.HudMessages;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.player.PlayerCyberData;
import com.waylays.chromeneon.common.registry.ModBlocks;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.registry.ModItems;
import com.waylays.chromeneon.server.CyberServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-game checks run on a real server ({@code runGameTestServer}): registrations, player data, RAM, the chair. */
@GameTestHolder(ChromeNeon.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "platform")
    public static void contentIsRegistered(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ChromeNeon.id("neural_chip")), "neural_chip item");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ChromeNeon.id("diagnostic_scanner")), "scanner item");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ChromeNeon.id("ripperdoc_chair")), "chair item");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ChromeNeon.id("ripperdoc_chair")), "chair block");
        helper.assertTrue(ModItems.RIPPERDOC_CHAIR.get().getBlock() == ModBlocks.RIPPERDOC_CHAIR.get(), "chair item places the chair");
        helper.assertTrue(ModImplants.REGISTRY.key().location().equals(ChromeNeon.id("implant")), "implant registry exists");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void chairIsPlacedWithAFacing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.RIPPERDOC_CHAIR.get().defaultBlockState()
                .setValue(RipperdocChairBlock.FACING, Direction.EAST));
        helper.assertBlockProperty(pos, RipperdocChairBlock.FACING, Direction.EAST);
        helper.succeed();
    }

    /** Uses the default config (4 cells, 60 ticks per cell): 240 ticks of regeneration. */
    @GameTest(template = "platform", timeoutTicks = 400)
    public static void ramRegeneratesUpToCapacity(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        int capacity = CyberData.ramCapacity(player);
        helper.assertTrue(capacity == ServerConfig.BASE_RAM.get(), "no implant: capacity is the base, got " + capacity);
        CyberData.get(player).ram().set(0, capacity);
        int needed = capacity * ServerConfig.RAM_REGEN_TICKS.get();
        // Mock players are not ticked by the server: drive the mod's real server tick handler ourselves
        helper.onEachTick(() -> CyberServerEvents.onTick(new PlayerTickEvent.Post(player)));
        helper.runAfterDelay(needed / 2 + 1, () -> {
            int half = CyberData.get(player).ram().cells();
            helper.assertTrue(half == capacity / 2, "halfway: " + capacity / 2 + " cells expected, got " + half);
        });
        helper.runAfterDelay(needed + 5, () -> {
            helper.assertTrue(CyberData.get(player).ram().cells() == capacity,
                    "RAM refilled to " + capacity + ", got " + CyberData.get(player).ram().cells());
            helper.succeed();
        });
    }

    @GameTest(template = "platform")
    public static void playerDataSurvivesSaveAndLoad(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PlayerCyberData data = CyberData.get(player);
        data.ram().set(3, 4);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());

        ServerPlayer other = helper.makeMockServerPlayerInLevel();
        other.load(saved);
        helper.assertTrue(CyberData.get(other).ram().cells() == 3, "RAM restored, got " + CyberData.get(other).ram().cells());

        Tag encoded = PlayerCyberData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        PlayerCyberData decoded = PlayerCyberData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(decoded.ram().cells() == 3, "codec round trip");
        helper.succeed();
    }

    /** Server systems can notify any player: connections without the mod's channel are skipped, not crashed. */
    @GameTest(template = "platform")
    public static void hudMessagesAreSafeForEveryPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        HudMessages.notify(player, NotificationKind.ALERT, Component.literal("test"), Component.literal("message"));
        CyberData.sync(player);
        helper.succeed();
    }
}
