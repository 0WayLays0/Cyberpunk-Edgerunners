package com.waylays.chromeneon.client.debug;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.chromeneon.ChromeNeon;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudBounds;
import com.waylays.chromeneon.client.hud.HudTime;
import com.waylays.chromeneon.client.hud.notify.IncomingCall;
import com.waylays.chromeneon.client.hud.notify.Notification;
import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.input.ChromeKeys;
import com.waylays.chromeneon.common.config.ClientConfig;
import com.waylays.chromeneon.common.hud.NotificationKind;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The automated HUD test (only with {@code -Dchromeneon.autotest=true}, see README): creates a survival world, puts
 * the player in every HUD state — effects, armor, targeting, entity and block scans, real damage from a source to
 * the side, critical health, notifications (one from the server), an incoming call, a glitch, RAM states, night,
 * minimal mode, north-up map, several window sizes and GUI scales, HUD off — saves a screenshot of each into
 * {@code chromeneon-autotest/}, checks that no HUD group covers the hotbar, the food bar, the crosshair or the
 * boss bar area, and logs the time the HUD takes per frame. Exits with status 0 on success, 1 on failure.
 */
@EventBusSubscriber(modid = ChromeNeon.MODID, value = Dist.CLIENT)
public final class HudAutoTest {
    private static final boolean ENABLED = Boolean.getBoolean("chromeneon.autotest");
    /** Waits {@code delay} ticks, then runs {@code action}. */
    private record Step(int delay, Consumer<Minecraft> action) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static final List<String> FAILURES = new ArrayList<>();
    private static int stage = -1, index, countdown, totalTicks;

    private HudAutoTest() {}

    private static void log(String msg, Object... args) {
        ChromeNeon.LOGGER.info("[HudAutoTest] " + msg, args);
    }

    private static void fail(String msg) {
        FAILURES.add(msg);
        ChromeNeon.LOGGER.error("[HudAutoTest] FAIL {}", msg);
    }

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Opening event) {
        // A fresh game directory opens on the accessibility onboarding screen instead of the title screen
        if (!ENABLED || stage != -1 || !(event.getNewScreen() instanceof TitleScreen
                || event.getNewScreen() instanceof AccessibilityOnboardingScreen)) return;
        stage = 0;
        Minecraft mc = Minecraft.getInstance();
        mc.options.onboardAccessibility = false;
        mc.options.pauseOnLostFocus = false;
        mc.options.renderDistance().set(6);
        mc.tell(() -> {
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            // A name of its own each run: map markers are kept per world name
            LevelSettings settings = new LevelSettings("chromeneon_autotest_" + System.currentTimeMillis(), GameType.SURVIVAL, false, Difficulty.NORMAL,
                    true, rules, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("chromeneon_autotest_" + System.currentTimeMillis(), settings,
                    new WorldOptions(20261125L, false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
        });
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED || stage < 0 || stage == 2) return;
        Minecraft mc = Minecraft.getInstance();
        if (++totalTicks > 20 * 60 * 15) {
            fail("timeout at step " + index);
            finish(mc);
            return;
        }
        try {
            if (stage == 0) {
                if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
                    log("joined the world");
                    plan();
                    stage = 1;
                    countdown = -1;
                }
                return;
            }
            clean(mc);
            if (index >= STEPS.size()) {
                finish(mc);
                return;
            }
            if (countdown < 0) countdown = STEPS.get(index).delay();
            if (countdown-- > 0) return;
            STEPS.get(index++).action().accept(mc);
            countdown = -1;
        } catch (Throwable t) {
            fail("step " + index + ": " + t);
            ChromeNeon.LOGGER.error("[HudAutoTest] exception", t);
            finish(mc);
        }
    }

    private static void step(int wait, Consumer<Minecraft> action) {
        STEPS.add(new Step(wait, action));
    }

    /** Runs a command as the player on the integrated server. */
    private static void command(Minecraft mc, String cmd) {
        IntegratedServer server = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), cmd);
        });
    }

    private static void onServerPlayer(Minecraft mc, Consumer<ServerPlayer> action) {
        IntegratedServer server = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) action.accept(p);
        });
    }

    private static void look(Minecraft mc, float yaw, float pitch) {
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
    }

    private static void plan() {
        STEPS.clear();
        step(10, mc -> resize(mc, 1920, 1080, 4));
        step(4, mc -> {
            applyScale(mc);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            command(mc, "gamerule sendCommandFeedback false");
            command(mc, "time set 6000");
            command(mc, "weather clear");
            command(mc, "tp @s ~ ~ ~ 0 0");
            // A clear arena in front of the player: a dark floor, nothing standing on it
            command(mc, "fill ~-12 ~ ~-4 ~12 ~12 ~16 minecraft:air");
            command(mc, "fill ~-12 ~-1 ~-4 ~12 ~-1 ~16 minecraft:polished_deepslate");
            command(mc, "item replace entity @s armor.chest with minecraft:iron_chestplate");
            command(mc, "item replace entity @s armor.head with minecraft:iron_helmet");
            command(mc, "effect give @s minecraft:resistance 900 4 true");
            // Effects with their icon but no particles (in first person they would float in front of the camera)
            onServerPlayer(mc, p -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 18000, 1, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 18000, 0, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 800, 0, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1, false, false, true));
            });
            command(mc, "chromeneon ram set @s 2");
        });
        step(10, mc -> {
            command(mc, "summon minecraft:husk ~ ~ ~6 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
            command(mc, "summon minecraft:cow ~5 ~ ~9 {NoAI:1b,PersistenceRequired:1b}");
            command(mc, "summon minecraft:villager ~-6 ~ ~4 {NoAI:1b,PersistenceRequired:1b}");
            command(mc, "summon minecraft:skeleton ~8 ~ ~1 {NoAI:1b,PersistenceRequired:1b}");
            command(mc, "setblock ~1 ~ ~2 minecraft:lever[face=floor,powered=true]");
            command(mc, "setblock ~-1 ~ ~2 minecraft:chest");
            String dim = mc.level.dimension().location().toString();
            ChromeHud.markers().add(dim, mc.player.blockPosition().offset(-20, 0, 14));
            ChromeHud.markers().add(dim, mc.player.blockPosition().offset(60, 0, -80));
        });
        step(60, mc -> {
            look(mc, 0, 8);
            // Plants cleared from the arena drop seeds the player picks up: not part of the picture
            command(mc, "kill @e[type=minecraft:item]");
            command(mc, "clear @s minecraft:wheat_seeds");
            HudPerf.reset();
        });
        step(30, mc -> shot(mc, "01_overview"));

        // Scan the husk (30 ticks), then let its data type in
        step(2, mc -> ChromeKeys.SCAN.setDown(true));
        step(62, mc -> shot(mc, "02_scan_entity"));
        step(2, mc -> look(mc, 10, 55));
        step(45, mc -> shot(mc, "03_scan_block"));
        step(2, mc -> ClientConfig.HUD_ENABLED.set(false));
        step(3, mc -> shot(mc, "dbg_03_hud_off"));
        step(2, mc -> ClientConfig.HUD_ENABLED.set(true));
        step(3, mc -> shot(mc, "dbg_03_hud_back"));
        step(2, mc -> {
            ChromeKeys.SCAN.setDown(false);
            look(mc, 0, 8);
        });

        // Real damage from the skeleton, on the player's left
        step(20, mc -> onServerPlayer(mc, p -> {
            Entity skeleton = p.serverLevel().getEntities(p, p.getBoundingBox().inflate(16),
                    e -> e.getType() == net.minecraft.world.entity.EntityType.SKELETON).stream().findFirst().orElse(null);
            p.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            p.invulnerableTime = 0;
            p.hurt(skeleton != null ? p.damageSources().mobAttack((net.minecraft.world.entity.LivingEntity) skeleton)
                    : p.damageSources().generic(), 7F);
        }));
        step(4, mc -> shot(mc, "04_damage"));
        step(5, mc -> onServerPlayer(mc, p -> p.setHealth(4F)));
        step(30, mc -> shot(mc, "05_critical"));
        step(5, mc -> {
            onServerPlayer(mc, p -> p.setHealth(p.getMaxHealth()));
            ChromeHud.notifications().clear();
        });

        step(20, mc -> {
            command(mc, "chromeneon notify @s cyberware Optiques Kiroshi detectees");
            ChromeHud.notify(NotificationKind.WARNING, Component.translatable("chromeneon.hud.test.warning.title"),
                    Component.translatable("chromeneon.hud.test.warning.message"));
            float start = HudTime.ticks();
            ChromeHud.notify(Notification.builder(NotificationKind.SCAN, Component.translatable("chromeneon.hud.test.upload.title"))
                    .message(Component.translatable("chromeneon.hud.test.upload.message")).icon(HudIcon.UPLOAD)
                    .progress(() -> Math.min(1.0, (HudTime.ticks() - start) / 100.0)).build());
        });
        step(25, mc -> shot(mc, "06_notifications"));
        step(5, mc -> ChromeHud.incomingCall(new IncomingCall(Component.literal("Kaito Vance"), null,
                Component.translatable("chromeneon.hud.call.channel"),
                () -> ChromeHud.notify(NotificationKind.SYSTEM_ON, Component.translatable("chromeneon.hud.call.accepted"),
                        Component.translatable("chromeneon.hud.call.test_only")), () -> {}, 400)));
        step(16, mc -> shot(mc, "07_call"));
        step(2, mc -> KeyMapping.click(ChromeKeys.ACCEPT_CALL.getKey()));
        step(20, mc -> {
            if (ChromeHud.calls().call() != null && ChromeHud.calls().ringing()) fail("the accept key did not answer the call");
        });

        step(5, mc -> ChromeHud.glitch(1F, 16));
        step(2, mc -> {
            shot(mc, "08_glitch");
            try {
                com.waylays.chromeneon.client.hud.fx.ScreenFxLayer.glitch().dump(
                        FMLPaths.GAMEDIR.get().resolve("chromeneon-autotest/dbg_08_glitch_capture.png"));
            } catch (java.io.IOException e) {
                fail("glitch capture dump: " + e);
            }
        });
        step(25, mc -> {
            command(mc, "chromeneon ram set @s 1");
            ChromeHud.ramInsufficient(3);
        });
        step(5, mc -> shot(mc, "09_ram_insufficient"));

        step(20, mc -> {
            command(mc, "time set 18000");
            ClientConfig.MINIMAL.set(true);
        });
        step(25, mc -> shot(mc, "10_night_minimal"));
        step(2, mc -> {
            ClientConfig.MINIMAL.set(false);
            ClientConfig.MINIMAP_ROTATE.set(false);
            ClientConfig.MINIMAP_ZOOM.set(2.0);
            look(mc, 135, 8);
        });
        step(40, mc -> shot(mc, "11_map_north_up_zoom2"));
        step(2, mc -> {
            ClientConfig.MINIMAP_ROTATE.set(true);
            ClientConfig.MINIMAP_ZOOM.set(1.0);
            look(mc, 0, 8);
            command(mc, "time set 6000");
            log("HUD time per frame (overview to here):\n{}", HudPerf.report());
            log("Minimap terrain sampling last tick: {} us", ChromeHud.terrain().lastTickNanos() / 1000);
        });

        step(2, mc -> {
            ChromeHud.incomingCall(new IncomingCall(Component.literal("Kaito Vance"), null,
                    Component.translatable("chromeneon.hud.call.channel"), () -> {}, () -> {}, 20 * 60 * 5));
            for (NotificationKind kind : new NotificationKind[] {NotificationKind.CYBERWARE, NotificationKind.WARNING, NotificationKind.INFO}) {
                ChromeHud.notify(Notification.builder(kind, Component.translatable("chromeneon.hud.test." + kind.getSerializedName() + ".title"))
                        .message(Component.translatable("chromeneon.hud.test." + kind.getSerializedName() + ".message"))
                        .duration(20 * 60 * 5).build());
            }
        });
        int[][] sizes = {{1280, 720, 2}, {1280, 720, 3}, {1920, 1080, 4}, {1920, 1080, 2}, {960, 540, 2}, {1280, 720, 1}};
        for (int[] s : sizes) {
            step(2, mc -> resize(mc, s[0], s[1], s[2]));
            step(4, HudAutoTest::applyScale);
            step(30, mc -> {
                checkLayout(mc, s[0] + "x" + s[1] + " gui " + s[2]);
                shot(mc, "res_" + s[0] + "x" + s[1] + "_gui" + s[2]);
            });
        }
        step(2, mc -> {
            resize(mc, 1920, 1080, 2);
            ClientConfig.HUD_SCALE.set(1.5);
        });
        step(4, HudAutoTest::applyScale);
        step(30, mc -> {
            checkLayout(mc, "1920x1080 gui 2, HUD scale 1.5");
            shot(mc, "res_1920x1080_gui2_hudscale1.5");
        });
        step(2, mc -> {
            ClientConfig.HUD_SCALE.set(1.0);
            ClientConfig.HUD_ENABLED.set(false);
            resize(mc, 1280, 720, 3);
        });
        step(4, HudAutoTest::applyScale);
        step(20, mc -> shot(mc, "12_hud_off_vanilla_back"));
        step(2, mc -> ClientConfig.HUD_ENABLED.set(true));
    }

    private static int pendingScale, pendingW, pendingH, retries;

    /** Asks for a window size; the GUI scale is applied by {@link #applyScale} once the window really has it. */
    private static void resize(Minecraft mc, int w, int h, int gui) {
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), w, h);
        pendingScale = gui;
        pendingW = w;
        pendingH = h;
        retries = 0;
    }

    /** The game caps the GUI scale to what the current window allows, so it is set after the resize. */
    private static void applyScale(Minecraft mc) {
        if ((mc.getWindow().getWidth() != pendingW || mc.getWindow().getHeight() != pendingH) && retries++ < 20) {
            // The window has not taken its new size yet: try again a little later
            STEPS.add(index, new Step(5, HudAutoTest::applyScale));
            return;
        }
        mc.options.guiScale().set(pendingScale);
        mc.resizeDisplay();
        if (mc.getWindow().getGuiScale() != pendingScale) fail("GUI scale " + pendingScale + " not applied, got " + mc.getWindow().getGuiScale());
    }

    /**
     * No group may cover what the game still draws itself: the hotbar, the experience bar, the food and air row
     * (right of centre; the left one held the hearts and armor this HUD replaces), the crosshair. The boss bar band
     * is reported only. Groups must not overlap each other either.
     */
    private static long lastFrames = -1;

    private static void checkLayout(Minecraft mc, String label) {
        if (HudBounds.frames() == lastFrames) fail(label + ": no frame was rendered since the last check");
        lastFrames = HudBounds.frames();
        if (HudBounds.snapshot().isEmpty()) fail(label + ": no HUD group drawn");
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        float cx = w / 2F;
        Map<String, HudBounds.Rect> essentials = Map.of(
                "hotbar", new HudBounds.Rect(cx - 91, h - 23, cx + 91, h),
                "experience bar", new HudBounds.Rect(cx - 91, h - 33, cx + 91, h - 23),
                "food and air", new HudBounds.Rect(cx + 10, h - 49, cx + 91, h - 33),
                "crosshair", new HudBounds.Rect(cx - 8, h / 2F - 8, cx + 8, h / 2F + 8),
                "boss bar", new HudBounds.Rect(cx - 91, 12, cx + 91, 31));
        HudBounds.snapshot().forEach((group, rect) -> {
            if (rect.x1() < 0 || rect.y1() < 0 || rect.x0() > w || rect.y0() > h) fail(label + ": " + group + " is off screen " + rect);
            essentials.forEach((name, area) -> {
                if (!rect.intersects(area)) return;
                if (name.equals("boss bar")) log("{}: {} reaches the boss bar band (only matters during boss fights) {}", label, group, rect);
                else fail(label + ": " + group + " covers the " + name + " " + rect);
            });
        });
        var groups = new java.util.ArrayList<>(HudBounds.snapshot().entrySet());
        for (int i = 0; i < groups.size(); i++) {
            for (int j = i + 1; j < groups.size(); j++) {
                if (groups.get(i).getValue().intersects(groups.get(j).getValue())) {
                    fail(label + ": " + groups.get(i).getKey() + " and " + groups.get(j).getKey() + " overlap");
                }
            }
        }
        log("{}: layout {}", label, HudBounds.snapshot());
    }

    /** Chat lines and vanilla toasts (advancements, tutorial) are not the HUD: cleared before each picture. */
    private static void clean(Minecraft mc) {
        mc.gui.getChat().clearMessages(false);
        mc.getToasts().clear();
    }

    private static void shot(Minecraft mc, String name) {
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("chromeneon-autotest");
            Files.createDirectories(dir);
            try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(dir.resolve(name + ".png"));
            }
            log("screenshot {} ({}x{}) hudTicks={} frameTime={} testTicks={} fps={} paused={} target={} scanning={} glitch={}", name,
                    mc.getWindow().getWidth(), mc.getWindow().getHeight(), HudTime.ticks(),
                    com.waylays.chromeneon.client.hud.HudFrame.current().now, totalTicks, mc.getFps(), mc.isPaused(),
                    describeTarget(mc), ChromeHud.target().scanning(), ChromeHud.glitch().strength(HudTime.ticks()));
        } catch (Exception e) {
            fail("screenshot " + name + ": " + e);
        }
    }

    private static String describeTarget(Minecraft mc) {
        var subject = ChromeHud.target().subject();
        if (subject == null) return "none";
        var f = com.waylays.chromeneon.client.hud.HudFrame.current();
        net.minecraft.world.phys.AABB box = subject instanceof com.waylays.chromeneon.client.hud.scan.ScanSubject.EntitySubject e
                ? e.entity().getBoundingBox() : new net.minecraft.world.phys.AABB(((com.waylays.chromeneon.client.hud.scan.ScanSubject.BlockSubject) subject).pos());
        float[] r = ChromeHud.projection().box(box, f.width, f.height);
        return subject.key() + " rect=" + java.util.Arrays.toString(r);
    }

    private static void finish(Minecraft mc) {
        stage = 2;
        if (FAILURES.isEmpty()) log("PASSED ({} steps)", STEPS.size());
        else log("FAILED: {}", FAILURES);
        int status = FAILURES.isEmpty() ? 0 : 1;
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            Runtime.getRuntime().halt(status);
        }, "chromeneon autotest exit");
        exit.setDaemon(true);
        exit.start();
    }
}
