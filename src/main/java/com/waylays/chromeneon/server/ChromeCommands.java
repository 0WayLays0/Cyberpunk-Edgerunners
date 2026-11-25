package com.waylays.chromeneon.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.chromeneon.common.config.ServerConfig;
import com.waylays.chromeneon.common.cyberware.InstallResult;
import com.waylays.chromeneon.common.hud.HudMessages;
import com.waylays.chromeneon.common.hud.NotificationKind;
import com.waylays.chromeneon.common.player.CyberData;
import com.waylays.chromeneon.common.player.CyberReport;
import com.waylays.chromeneon.common.player.PlayerCyberData;
import com.waylays.chromeneon.common.registry.ModImplants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * {@code /chromeneon}: status for everyone about themselves; RAM, implants and HUD notifications for operators
 * (testing and server administration, until the ripperdoc chair handles installs).
 */
public final class ChromeCommands {
    private ChromeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chromeneon")
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> status(ctx, EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("ram").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("cells", IntegerArgumentType.integer(0))
                                                .executes(ChromeCommands::setRam)))))
                .then(Commands.literal("notify").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("kind", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(NotificationKind.values()).map(NotificationKind::getSerializedName), builder))
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(ChromeCommands::notifyPlayers)))))
                .then(Commands.literal("implant").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("install").then(implantArgs(true)))
                        .then(Commands.literal("remove").then(implantArgs(false)))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> implantArgs(boolean install) {
        return Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("implant", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(ModImplants.REGISTRY.keySet(), builder))
                        .executes(ctx -> install ? install(ctx) : remove(ctx)));
    }

    private static int notifyPlayers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "kind");
        NotificationKind kind = Arrays.stream(NotificationKind.values()).filter(k -> k.getSerializedName().equals(name))
                .findFirst().orElse(null);
        if (kind == null) {
            ctx.getSource().sendFailure(Component.translatable("chromeneon.command.unknown_kind", name));
            return 0;
        }
        Component message = Component.literal(StringArgumentType.getString(ctx, "message"));
        var targets = EntityArgument.getPlayers(ctx, "targets");
        targets.forEach(p -> HudMessages.notify(p, kind, Component.translatable("chromeneon.notify.kind." + name), message));
        ctx.getSource().sendSuccess(() -> Component.translatable("chromeneon.command.notified", targets.size()), true);
        return targets.size();
    }

    private static int status(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        CyberReport.of(target).forEach(line -> ctx.getSource().sendSuccess(() -> line, false));
        return 1;
    }

    private static int setRam(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        int capacity = CyberData.ramCapacity(target);
        CyberData.get(target).ram().set(IntegerArgumentType.getInteger(ctx, "cells"), capacity);
        CyberData.sync(target);
        int cells = CyberData.get(target).ram().cells();
        ctx.getSource().sendSuccess(() -> Component.translatable("chromeneon.command.ram_set",
                target.getDisplayName(), cells, capacity), true);
        return cells;
    }

    private static int install(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "implant");
        InstallResult result = CyberData.get(target).loadout().install(id, ModImplants::get, ServerConfig.MAX_HUMANITY.get());
        if (!result.ok()) {
            ctx.getSource().sendFailure(Component.translatable(result.translationKey(), id.toString()));
            return 0;
        }
        CyberData.sync(target);
        HudMessages.notify(target, NotificationKind.CYBERWARE, Component.translatable("chromeneon.notify.cyberware.installed"),
                Component.literal(id.toString()));
        ctx.getSource().sendSuccess(() -> Component.translatable("chromeneon.command.installed", id.toString(),
                target.getDisplayName()), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "implant");
        PlayerCyberData data = CyberData.get(target);
        if (!data.uninstall(id)) {
            ctx.getSource().sendFailure(Component.translatable("chromeneon.command.not_installed", id.toString()));
            return 0;
        }
        CyberData.sync(target);
        HudMessages.notify(target, NotificationKind.SYSTEM_OFF, Component.translatable("chromeneon.notify.cyberware.removed"),
                Component.literal(id.toString()));
        ctx.getSource().sendSuccess(() -> Component.translatable("chromeneon.command.removed", id.toString(),
                target.getDisplayName()), true);
        return 1;
    }
}
