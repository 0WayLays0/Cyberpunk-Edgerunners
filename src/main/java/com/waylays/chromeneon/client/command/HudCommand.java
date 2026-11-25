package com.waylays.chromeneon.client.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.waylays.chromeneon.client.hud.ChromeHud;
import com.waylays.chromeneon.client.hud.HudTime;
import com.waylays.chromeneon.client.hud.notify.IncomingCall;
import com.waylays.chromeneon.client.hud.notify.Notification;
import com.waylays.chromeneon.client.hud.theme.HudIcon;
import com.waylays.chromeneon.client.hud.theme.NeonPalette;
import com.waylays.chromeneon.common.hud.NotificationKind;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Arrays;
import java.util.Locale;

/**
 * {@code /cnhud}: shows HUD states on demand, for testing and for checking the look without waiting for the
 * situation. It only drives the HUD on this client (nothing on the server changes). Real damage, effects and
 * server notifications are tested with {@code /damage}, {@code /effect} and {@code /chromeneon notify}.
 */
public final class HudCommand {
    private HudCommand() {}

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cnhud")
                .then(Commands.literal("notify")
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(NotificationKind.values()).map(NotificationKind::getSerializedName), b))
                                .executes(HudCommand::notifyKind)))
                .then(Commands.literal("call")
                        .executes(c -> call(c, "Kaito Vance", null))
                        .then(Commands.argument("caller", StringArgumentType.greedyString())
                                .executes(c -> call(c, StringArgumentType.getString(c, "caller"), StringArgumentType.getString(c, "caller")))))
                .then(Commands.literal("glitch")
                        .executes(c -> glitch(1F))
                        .then(Commands.argument("intensity", FloatArgumentType.floatArg(0F, 1F))
                                .executes(c -> glitch(FloatArgumentType.getFloat(c, "intensity")))))
                .then(Commands.literal("ram")
                        .then(Commands.argument("cost", IntegerArgumentType.integer(1, 64))
                                .executes(c -> {
                                    ChromeHud.ramInsufficient(IntegerArgumentType.getInteger(c, "cost"));
                                    return 1;
                                })))
                .then(Commands.literal("upload").executes(HudCommand::upload))
                .then(Commands.literal("ping").executes(c -> {
                    var player = Minecraft.getInstance().player;
                    if (player != null) ChromeHud.ping(player.position(), 10F, NeonPalette.CYAN);
                    return 1;
                })));
    }

    private static int notifyKind(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "kind").toLowerCase(Locale.ROOT);
        NotificationKind kind = Arrays.stream(NotificationKind.values()).filter(k -> k.getSerializedName().equals(name))
                .findFirst().orElse(NotificationKind.INFO);
        ChromeHud.notify(kind, Component.translatable("chromeneon.hud.test." + kind.getSerializedName() + ".title"),
                Component.translatable("chromeneon.hud.test." + kind.getSerializedName() + ".message"));
        return 1;
    }

    private static int call(CommandContext<CommandSourceStack> c, String caller, String playerName) {
        ChromeHud.incomingCall(new IncomingCall(Component.literal(caller), playerName,
                Component.translatable("chromeneon.hud.call.channel"),
                () -> ChromeHud.notify(NotificationKind.SYSTEM_ON, Component.translatable("chromeneon.hud.call.accepted"),
                        Component.translatable("chromeneon.hud.call.test_only")),
                () -> {}, 400));
        return 1;
    }

    private static int glitch(float intensity) {
        ChromeHud.glitch(intensity, 16);
        return 1;
    }

    /** A notification following a progress: the shape uploads will take in the hacking phase. */
    private static int upload(CommandContext<CommandSourceStack> c) {
        float start = HudTime.ticks();
        ChromeHud.notify(Notification.builder(NotificationKind.SCAN, Component.translatable("chromeneon.hud.test.upload.title"))
                .message(Component.translatable("chromeneon.hud.test.upload.message"))
                .icon(HudIcon.UPLOAD)
                .progress(() -> Math.min(1.0, (HudTime.ticks() - start) / 100.0))
                .build());
        return 1;
    }
}
