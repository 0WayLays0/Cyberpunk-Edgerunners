package com.waylays.chromeneon.gametest;

import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.common.block.RipperdocChairBlock;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.Cyberware;
import com.waylays.chromeneon.common.cyberware.CyberwareService;
import com.waylays.chromeneon.common.cyberware.Implant;
import com.waylays.chromeneon.common.cyberware.ImplantContext;
import com.waylays.chromeneon.common.cyberware.InstallResult;
import com.waylays.chromeneon.common.cyberware.UseResult;
import com.waylays.chromeneon.common.cyberware.implants.ChronosAccelerator;
import com.waylays.chromeneon.common.cyberware.mechanics.AirJump;
import com.waylays.chromeneon.common.cyberware.mechanics.BladeCombat;
import com.waylays.chromeneon.common.cyberware.mechanics.Malfunctions;
import com.waylays.chromeneon.common.cyberware.mechanics.TimeDilation;
import com.waylays.chromeneon.common.network.CyberwareRequestPayload;
import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.player.PlayerCyberData;
import com.waylays.chromeneon.common.registry.ModBlocks;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.registry.ModItems;
import com.waylays.chromeneon.server.CyberServerEvents;
import com.waylays.chromeneon.server.CyberwareRequests;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Cyberware on a real (dedicated) server: surgery rules, persistence, and each implant's effect measured on the game
 * itself (damage taken, health restored, attributes, entity movement), not on the implant's own bookkeeping.
 */
@GameTestHolder(ChromeNeon.MODID)
@PrefixGameTestTemplate(false)
public final class CyberwareGameTests {
    private CyberwareGameTests() {}

    @SuppressWarnings("removal")
    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        // A player who just joined cannot be hurt for 3 seconds: let that pass (the server-side part of a tick only)
        for (int i = 0; i < 61; i++) player.tick();
        return player;
    }

    /** Mock players are not ticked by the server: run the mod's real player tick on them. */
    private static void tickEachTick(GameTestHelper helper, ServerPlayer player) {
        helper.onEachTick(() -> CyberServerEvents.onTick(new PlayerTickEvent.Post(player)));
    }

    private static void grant(GameTestHelper helper, ServerPlayer player, Implant implant, ResourceLocation slot) {
        InstallResult r = CyberwareService.grant(player, ModImplants.id(implant), slot);
        helper.assertTrue(r.ok(), "grant " + ModImplants.id(implant) + " -> " + r);
    }

    @GameTest(template = "platform")
    public static void surgeryNeedsTheChairAndCalm(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        PlayerCyberData data = CyberData.get(player);
        player.getInventory().setItem(0, new ItemStack(ModItems.REAPER_BLADES.get()));
        helper.assertTrue(CyberwareService.install(player, false, ModBodySlots.LEFT_ARM, 0) == InstallResult.NOT_SEATED, "not seated");
        data.lastHurtAt = player.level().getGameTime();
        helper.assertTrue(CyberwareService.install(player, true, ModBodySlots.LEFT_ARM, 0) == InstallResult.IN_COMBAT, "in combat");
        data.lastHurtAt = -10_000L;
        helper.assertTrue(CyberwareService.install(player, true, ModBodySlots.SYSTEM, 0) == InstallResult.WRONG_SLOT, "blades in the spine");
        helper.assertTrue(CyberwareService.install(player, true, ModBodySlots.LEFT_ARM, 1) == InstallResult.NO_ITEM, "empty inventory slot");
        helper.assertTrue(CyberwareService.install(player, true, ModBodySlots.LEFT_ARM, 0) == InstallResult.OK, "install");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "the implant item is used up");
        helper.assertTrue(data.loadout().contains(ModImplants.id(ModImplants.REAPER_BLADES.get())), "installed");
        helper.assertTrue(CyberwareService.remove(player, false, ModBodySlots.LEFT_ARM, 0) == InstallResult.NOT_SEATED, "removal needs the chair too");
        helper.assertTrue(CyberwareService.remove(player, true, ModBodySlots.LEFT_ARM, 0) == InstallResult.OK, "remove");
        helper.assertTrue(player.getInventory().countItem(ModItems.REAPER_BLADES.get()) == 1, "the implant comes back");
        helper.assertTrue(data.loadout().entries().isEmpty(), "body empty again");
        helper.succeed();
    }

    /** The real path: sit in a placed chair, then the client's request is checked by the server. */
    @GameTest(template = "platform")
    public static void chairSeatsThePlayerAndAllowsRequests(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        BlockPos chair = new BlockPos(2, 2, 2);
        helper.setBlock(chair, ModBlocks.RIPPERDOC_CHAIR.get());
        player.getInventory().setItem(3, new ItemStack(ModItems.ARGUS_OPTICS.get()));
        var request = new CyberwareRequestPayload(CyberwareRequestPayload.Action.INSTALL, ModBodySlots.OPTICS, 0, 3, false);
        CyberwareRequests.handle(player, request);
        helper.assertFalse(CyberData.get(player).loadout().contains(ModImplants.id(ModImplants.ARGUS_OPTICS.get())), "refused standing");
        helper.useBlock(chair, player);
        helper.assertTrue(helper.absolutePos(chair).equals(RipperdocChairBlock.seatedIn(player)), "seated in the chair");
        CyberwareRequests.handle(player, request);
        helper.assertTrue(CyberData.get(player).loadout().contains(ModImplants.id(ModImplants.ARGUS_OPTICS.get())), "installed seated");
        player.stopRiding();
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void oneOperatingSystemAndHumanityLimits(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        grant(helper, player, ModImplants.CHRONOS_MK1.get(), ModBodySlots.SYSTEM);
        InstallResult second = CyberwareService.grant(player, ModImplants.id(ModImplants.CHRONOS_MK3.get()), ModBodySlots.SYSTEM);
        helper.assertTrue(second == InstallResult.SLOT_FULL, "a second system: " + second);
        helper.assertTrue(CyberData.stats(player).humanity() == ServerConfig.MAX_HUMANITY.get() - 18, "Mk.I costs 18 humanity");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void cyberwareSurvivesSaveAndLoad(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        grant(helper, player, ModImplants.CHRONOS_MK2.get(), ModBodySlots.SYSTEM);
        grant(helper, player, ModImplants.REAPER_BLADES.get(), ModBodySlots.RIGHT_ARM);
        grant(helper, player, ModImplants.CARAPACE_DERMIS.get(), ModBodySlots.INTEGUMENTARY);
        PlayerCyberData data = CyberData.get(player);
        data.loadout().in(ModBodySlots.RIGHT_ARM).getFirst().state().enabled = false;
        data.loadout().in(ModBodySlots.SYSTEM).getFirst().state().cooldownUntil = 4321;
        data.addCapacity(4, 16);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer other = survivor(helper);
        other.load(saved);
        PlayerCyberData back = CyberData.get(other);
        helper.assertTrue(back.loadout().entries().size() == 3, "three implants restored");
        helper.assertFalse(back.loadout().in(ModBodySlots.RIGHT_ARM).getFirst().state().enabled, "switch restored");
        helper.assertTrue(back.loadout().in(ModBodySlots.SYSTEM).getFirst().state().cooldownUntil == 4321, "cooldown restored");
        helper.assertTrue(back.capacityBonus() == 4, "capacity restored");
        helper.assertTrue(back.loadout().in(ModBodySlots.INTEGUMENTARY).getFirst().state().resource > 0F, "plating integrity restored");
        helper.succeed();
    }

    /** Same hit on two players: the plating takes its share of it. */
    @GameTest(template = "platform")
    public static void dermisAbsorbsRealDamage(GameTestHelper helper) {
        ServerPlayer plated = survivor(helper), bare = survivor(helper);
        grant(helper, plated, ModImplants.CARAPACE_DERMIS.get(), ModBodySlots.INTEGUMENTARY);
        plated.hurt(plated.damageSources().cactus(), 10F);
        bare.hurt(bare.damageSources().cactus(), 10F);
        float expected = 20F - 10F * (1F - ServerConfig.DERMIS_SHARE.get().floatValue());
        helper.assertTrue(Math.abs(bare.getHealth() - 10F) < 0.01F, "control took 10, health " + bare.getHealth());
        helper.assertTrue(Math.abs(plated.getHealth() - expected) < 0.01F, "plated health " + plated.getHealth() + ", expected " + expected);
        float integrity = CyberData.get(plated).loadout().in(ModBodySlots.INTEGUMENTARY).getFirst().state().resource;
        helper.assertTrue(Math.abs(integrity - (ServerConfig.DERMIS_INTEGRITY.get() - 10 * ServerConfig.DERMIS_SHARE.get())) < 0.01, "integrity " + integrity);
        // Fire goes through
        float before = plated.getHealth();
        plated.invulnerableTime = 0;
        plated.hurt(plated.damageSources().inFire(), 2F);
        helper.assertTrue(Math.abs(plated.getHealth() - (before - 2F)) < 0.01F, "fire is not absorbed");
        helper.succeed();
    }

    @GameTest(template = "platform", timeoutTicks = 120)
    public static void pumpInjectsAfterARealHit(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        grant(helper, player, ModImplants.HEMOSTAT_PUMP.get(), ModBodySlots.CIRCULATORY);
        player.hurt(player.damageSources().cactus(), 16F);
        float half = ServerConfig.PUMP_HEAL.get().floatValue() / 2F;
        helper.assertTrue(Math.abs(player.getHealth() - (4F + half)) < 0.01F, "half injected at once: " + player.getHealth());
        ImplantContext pump = Cyberware.first(player, ModImplants.HEMOSTAT_PUMP.get()).orElseThrow();
        helper.assertTrue(pump.state().charges == ServerConfig.PUMP_CHARGES.get() - 1, "a charge used");
        tickEachTick(helper, player);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(Math.abs(player.getHealth() - (4F + 2 * half)) < 0.05F, "the rest over time: " + player.getHealth());
            helper.succeed();
        });
    }

    @GameTest(template = "platform")
    public static void bladesArmAnEmptyHand(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        grant(helper, player, ModImplants.REAPER_BLADES.get(), ModBodySlots.LEFT_ARM);
        grant(helper, player, ModImplants.REAPER_BLADES.get(), ModBodySlots.RIGHT_ARM);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(CyberwareService.useAll(player, ModImplants.REAPER_BLADES.get()).ok(), "deploy");
        BladeCombat.tick(player, CyberData.get(player));
        double armed = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(Math.abs(armed - base - 2 * ServerConfig.BLADE_DAMAGE.get()) < 1e-6, "two sets: +" + (armed - base));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        BladeCombat.tick(player, CyberData.get(player));
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - base) < 1e-6, "no bonus with something in hand");
        // Switched off: cannot be deployed again
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        CyberwareService.useAll(player, ModImplants.REAPER_BLADES.get());
        CyberwareService.setEnabled(player, ModBodySlots.LEFT_ARM, 0, false);
        helper.assertTrue(CyberwareService.use(player, ModImplants.REAPER_BLADES.get()) == UseResult.DISABLED, "disabled");
        helper.succeed();
    }

    /** A free projectile near the player flies about four times slower while the Mk.II runs. */
    @GameTest(template = "platform", timeoutTicks = 100)
    public static void acceleratorSlowsTheWorldAround(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        grant(helper, player, ModImplants.CHRONOS_MK2.get(), ModBodySlots.SYSTEM);
        CyberData.get(player).ram().set(8, 8);
        helper.assertTrue(CyberwareService.use(player, ChronosAccelerator.class).ok(), "start");
        helper.assertTrue(TimeDilation.slowdown(player) == ServerConfig.CHRONOS_MK2.slowdown().get(), "the Mk.II zone runs");
        Snowball ball = helper.spawn(EntityType.SNOWBALL, new Vec3(1.5, 5, 1.5));
        ball.setNoGravity(true);
        ball.setDeltaMovement(0.25, 0, 0);
        Vec3 start = ball.position();
        tickEachTick(helper, player);
        helper.runAfterDelay(40, () -> {
            double moved = ball.position().distanceTo(start);
            // Unslowed: about 8.3 blocks in 40 ticks (0.25 per tick with drag); one tick in four: about 2.4
            helper.assertTrue(moved > 0.5 && moved < 4.5, "moved " + moved + " blocks in 40 ticks");
            helper.succeed();
        });
    }

    @GameTest(template = "platform")
    public static void acceleratorCooldownIsShortenedByTheCoprocessor(GameTestHelper helper) {
        ServerPlayer plain = survivor(helper), boosted = survivor(helper);
        for (ServerPlayer p : new ServerPlayer[] {plain, boosted}) {
            grant(helper, p, ModImplants.CHRONOS_MK2.get(), ModBodySlots.SYSTEM);
            CyberData.get(p).ram().set(8, 8);
        }
        grant(helper, boosted, ModImplants.SYNAPSE_COPROCESSOR.get(), ModBodySlots.BIOCHIP);
        long now = plain.level().getGameTime();
        int base = ServerConfig.CHRONOS_MK2.cooldown().get();
        for (ServerPlayer p : new ServerPlayer[] {plain, boosted}) {
            helper.assertTrue(CyberwareService.use(p, ChronosAccelerator.class).ok(), "start");
            helper.assertTrue(CyberwareService.use(p, ChronosAccelerator.class).ok(), "stopped early by hand");
            helper.assertFalse(TimeDilation.isDilating(p), "the zone is gone");
        }
        long plainCd = Cyberware.first(plain, ModImplants.CHRONOS_MK2.get()).orElseThrow().state().cooldownUntil - now;
        long boostedCd = Cyberware.first(boosted, ModImplants.CHRONOS_MK2.get()).orElseThrow().state().cooldownUntil - now;
        helper.assertTrue(plainCd == base, "plain cooldown " + plainCd);
        helper.assertTrue(boostedCd == Math.round(base * (1 - ServerConfig.COPROCESSOR_COOLDOWN.get())), "with coprocessor " + boostedCd);
        helper.assertTrue(CyberwareService.use(plain, ChronosAccelerator.class) == UseResult.COOLDOWN, "cooling down");
        helper.succeed();
    }

    /** With the chance set to 1 for the test, every hit while dilated is critical: damage x (1 + bonus). */
    @GameTest(template = "platform")
    public static void acceleratorMakesHitsCritical(GameTestHelper helper) {
        var chance = ServerConfig.CHRONOS_MK2.critChance();
        double previous = chance.get();
        chance.set(1.0);
        try {
            ServerPlayer player = survivor(helper);
            grant(helper, player, ModImplants.CHRONOS_MK2.get(), ModBodySlots.SYSTEM);
            CyberData.get(player).ram().set(8, 8);
            Pig idle = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
            Pig hit = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 3));
            idle.hurt(player.damageSources().playerAttack(player), 4F);
            helper.assertTrue(Math.abs(idle.getHealth() - (idle.getMaxHealth() - 4F)) < 0.01F, "no crit before: " + idle.getHealth());
            helper.assertTrue(CyberwareService.use(player, ChronosAccelerator.class).ok(), "start");
            hit.hurt(player.damageSources().playerAttack(player), 4F);
            float dealt = hit.getMaxHealth() - hit.getHealth();
            float expected = 4F * (1F + ServerConfig.CHRONOS_MK2.critDamage().get().floatValue());
            helper.assertTrue(Math.abs(dealt - expected) < 0.01F, "critical hit dealt " + dealt + ", expected " + expected);
        } finally {
            chance.set(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void airJumpsAreCheckedByTheServer(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        grant(helper, player, ModImplants.SPRING_TENDONS.get(), ModBodySlots.LEGS);
        player.setOnGround(true);
        CyberServerEvents.onTick(new PlayerTickEvent.Post(player));
        int allowed = ServerConfig.EXTRA_JUMPS.get();
        helper.assertTrue(AirJump.jump(player) == UseResult.NOT_NOW, "on the ground");
        player.setOnGround(false);
        player.fallDistance = 6F;
        for (int i = 0; i < allowed; i++) helper.assertTrue(AirJump.jump(player).ok(), "air jump " + (i + 1));
        helper.assertTrue(player.fallDistance == 0F, "the fall starts over");
        helper.assertTrue(AirJump.jump(player) == UseResult.NO_CHARGE, "no more jumps until landing");
        player.setOnGround(true);
        CyberServerEvents.onTick(new PlayerTickEvent.Post(player));
        player.setOnGround(false);
        helper.assertTrue(AirJump.jump(player).ok(), "landing refills");
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void malfunctionStopsAnImplant(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        grant(helper, player, ModImplants.REAPER_BLADES.get(), ModBodySlots.LEFT_ARM);
        CyberwareService.use(player, ModImplants.REAPER_BLADES.get());
        ImplantContext ctx = Cyberware.first(player, ModImplants.REAPER_BLADES.get()).orElseThrow();
        Malfunctions.trigger(ctx, 100);
        helper.assertFalse(ctx.state().active, "retracted by the malfunction");
        helper.assertTrue(CyberwareService.use(player, ModImplants.REAPER_BLADES.get()) == UseResult.MALFUNCTION, "cannot be used");
        helper.succeed();
    }
}
