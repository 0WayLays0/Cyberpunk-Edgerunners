package com.waylays.chromeneon.client.debug;

import com.waylays.chromeneon.client.cyberware.ClientCyberware;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.input.ChromeKeys;
import com.waylays.chromeneon.client.screen.CyberwareScreen;
import com.waylays.chromeneon.client.screen.RipperdocScreen;
import com.waylays.chromeneon.common.block.RipperdocChairBlock;
import com.waylays.chromeneon.common.item.ImplantItem;
import com.waylays.chromeneon.common.registry.ModBodySlots;
import com.waylays.chromeneon.common.registry.ModImplants;
import com.waylays.chromeneon.common.registry.ModItems;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static com.waylays.chromeneon.client.debug.HudAutoTest.applyScale;
import static com.waylays.chromeneon.client.debug.HudAutoTest.command;
import static com.waylays.chromeneon.client.debug.HudAutoTest.fail;
import static com.waylays.chromeneon.client.debug.HudAutoTest.log;
import static com.waylays.chromeneon.client.debug.HudAutoTest.look;
import static com.waylays.chromeneon.client.debug.HudAutoTest.onServerPlayer;
import static com.waylays.chromeneon.client.debug.HudAutoTest.resize;
import static com.waylays.chromeneon.client.debug.HudAutoTest.shot;
import static com.waylays.chromeneon.client.debug.HudAutoTest.step;

/**
 * The cyberware part of the in-game test, played like a player would: sit in a ripperdoc chair, install implants
 * through the surgery screen (the requests go to the server, which checks them), read the body screen, deploy the
 * blades, run the accelerator while moving (afterimages), use the optics. Each stage is checked against what the
 * server synced back, and pictured.
 */
final class CyberwareAutoTest {
    /** Chair position, relative to where the player stands when this part starts. */
    private static BlockPos origin, chair;

    private CyberwareAutoTest() {}

    private static int slotOf(Minecraft mc, Item item) {
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) return i;
        return -1;
    }

    private static void expectInstalled(Minecraft mc, String what, int count) {
        int n = ClientCyberware.entries().size();
        if (n != count) fail(what + ": " + count + " implants expected on the client, got " + n);
        else log("{}: {} implants installed and synced", what, n);
    }

    static void plan() {
        step(2, mc -> resize(mc, 1920, 1080, 3));
        step(4, mc -> {
            applyScale(mc);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            command(mc, "kill @e[type=!minecraft:player]");
            // What the first part of the test left on screen (messages, the call) is not part of these pictures
            ChromeHud.notifications().clear();
            ChromeHud.calls().clear();
            mc.player.getInventory().selected = 8;
            command(mc, "effect clear @s minecraft:slowness");
            command(mc, "effect clear @s minecraft:weakness");
            command(mc, "chromeneon ram set @s 16");
            command(mc, "chromeneon capacity @s 16");
            origin = mc.player.blockPosition();
            chair = origin.offset(-3, 0, 3);
            command(mc, "setblock " + chair.getX() + " " + chair.getY() + " " + chair.getZ() + " chromeneon:ripperdoc_chair[facing=east]");
            for (String item : new String[] {"chronos_mk2", "reaper_blades", "reaper_blades", "synapse_coprocessor", "spring_tendons", "carapace_dermis"}) {
                command(mc, "give @s chromeneon:" + item);
            }
            command(mc, "summon minecraft:husk ~3 ~ ~9 {NoAI:1b,PersistenceRequired:1b,Rotation:[160f,0f]}");
            command(mc, "summon minecraft:pillager ~-2 ~ ~11 {NoAI:1b,PersistenceRequired:1b,Rotation:[200f,0f]}");
        });
        step(10, mc -> {
            command(mc, "kill @e[type=minecraft:item]");
            // Close to the chair, looking down at it
            mc.player.setPos(Vec3.atBottomCenterOf(chair).add(2.2, 0, -1.6));
            look(mc, 55, 38);
        });
        step(40, mc -> shot(mc, "20_ripperdoc_chair"));

        // Sit: the same server path as a right click on the chair
        step(2, mc -> onServerPlayer(mc, p -> {
            var state = p.level().getBlockState(chair);
            if (!(state.getBlock() instanceof RipperdocChairBlock)) {
                fail("no chair at " + chair);
                return;
            }
            state.useWithoutItem(p.level(), p, new BlockHitResult(Vec3.atCenterOf(chair), Direction.UP, chair, false));
        }));
        step(20, mc -> {
            if (!(mc.screen instanceof RipperdocScreen)) fail("the chair did not open the surgery screen: " + mc.screen);
            if (!(mc.player.getVehicle() != null)) fail("not seated");
        });
        step(10, mc -> shot(mc, "21_ripperdoc_carried"));
        install(ModItems.CHRONOS_MK2.get(), false);
        install(ModItems.REAPER_BLADES.get(), false);
        install(ModItems.REAPER_BLADES.get(), false);
        install(ModItems.SYNAPSE_COPROCESSOR.get(), false);
        install(ModItems.SPRING_TENDONS.get(), true);
        step(10, mc -> expectInstalled(mc, "after surgery", 6));
        // The plating is chosen but not installed: the screen shows its verdict and costs
        step(2, mc -> {
            if (mc.screen instanceof RipperdocScreen screen) screen.choose(slotOf(mc, ModItems.CARAPACE_DERMIS.get()));
        });
        step(10, mc -> shot(mc, "23_ripperdoc_verdict"));

        // Up from the chair, then the body screen, open anywhere
        step(2, mc -> {
            mc.setScreen(null);
            onServerPlayer(mc, p -> p.stopRiding());
        });
        step(10, mc -> {
            CyberwareScreen screen = new CyberwareScreen();
            mc.setScreen(screen);
            screen.select(ModBodySlots.SYSTEM, 0);
        });
        step(15, mc -> shot(mc, "24_cyberware_body"));
        step(2, mc -> {
            if (mc.screen instanceof CyberwareScreen screen) screen.select(ModBodySlots.LEFT_ARM, 0);
        });
        step(10, mc -> shot(mc, "25_cyberware_blades_sheet"));
        step(2, mc -> {
            mc.setScreen(null);
            mc.player.getInventory().selected = 8;
            mc.player.setPos(Vec3.atBottomCenterOf(origin));
            look(mc, 0, 6);
        });
        step(15, mc -> shot(mc, "26_hud_implants"));

        // Blades out
        step(2, mc -> KeyMapping.click(ChromeKeys.BLADES.getKey()));
        step(20, mc -> {
            boolean out = ClientCyberware.activeAndWorking(ModImplants.REAPER_BLADES.get());
            if (!out) fail("blades not deployed after the key");
            shot(mc, "27_blades_first_person");
        });
        step(2, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        step(10, mc -> shot(mc, "28_blades_third_person"));

        // The accelerator, moving sideways so the afterimages trail
        step(2, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.setPos(Vec3.atBottomCenterOf(origin.offset(6, 0, 2)));
            look(mc, 0, 12);
        });
        step(5, mc -> KeyMapping.click(ChromeKeys.ACCELERATOR.getKey()));
        step(4, mc -> {
            if (!ClientCyberware.dilating()) fail("the accelerator did not start");
            mc.options.keyRight.setDown(true);
            mc.options.keySprint.setDown(true);
        });
        step(14, mc -> shot(mc, "29_chronos_afterimages"));
        step(1, mc -> {
            mc.options.keyRight.setDown(false);
            mc.options.keySprint.setDown(false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            look(mc, 10, 8);
        });
        step(6, mc -> shot(mc, "30_chronos_first_person"));
        step(2, mc -> KeyMapping.click(ChromeKeys.ACCELERATOR.getKey()));
        step(10, mc -> {
            if (ClientCyberware.dilating()) fail("pressing again did not stop the accelerator");
            ClientCyberware.accelerator().ifPresent(f -> {
                long cd = f.state().cooldownUntil - ClientCyberware.now();
                log("accelerator stopped by hand, cooldown left {} ticks (coprocessor installed)", cd);
            });
            shot(mc, "31_chronos_cooldown");
        });

        // Optics: zoom, then a tactical analysis
        step(2, mc -> {
            KeyMapping.click(ChromeKeys.BLADES.getKey());
            ChromeKeys.ZOOM.setDown(true);
            look(mc, 12, 4);
        });
        step(20, mc -> shot(mc, "32_optics_zoom"));
        step(2, mc -> {
            ChromeKeys.ZOOM.setDown(false);
            KeyMapping.click(ChromeKeys.ANALYSIS.getKey());
        });
        step(20, mc -> {
            if (!ClientCyberware.analysing()) fail("tactical analysis not running");
            shot(mc, "33_optics_tactical");
        });
    }

    /** Chooses the carried implant, lets the screen pick its socket, installs it, waits for the server's answer. */
    private static void install(Item item, boolean picture) {
        step(2, mc -> {
            if (!(mc.screen instanceof RipperdocScreen screen)) {
                fail("surgery screen closed before installing " + item);
                return;
            }
            int slot = slotOf(mc, item);
            if (slot < 0 || !(mc.player.getInventory().getItem(slot).getItem() instanceof ImplantItem)) {
                fail("not carried: " + item);
                return;
            }
            screen.choose(slot);
        });
        if (picture) step(6, mc -> shot(mc, "22_ripperdoc_selected"));
        step(4, mc -> {
            if (mc.screen instanceof RipperdocScreen screen) screen.installChosen();
        });
    }
}
