package net.just_s.sframes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class FrameCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                CommandManager.literal("sframes").
                        then(
                                CommandManager.literal("color").then(
                                        CommandManager.argument("value", ColorArgumentType.color()).executes(
                                                (context) -> executeColor(context.getSource(), ColorArgumentType.getColor(context, "value")))
                                )
                        ).
                        then(
                                CommandManager.literal("baseColor").requires((source) -> source.hasPermissionLevel(3)).then(
                                        CommandManager.argument("value", ColorArgumentType.color()).executes(
                                                (context) -> executeBaseColor(context.getSource(), ColorArgumentType.getColor(context, "value")))
                                )
                        ).
                        then(
                                CommandManager.literal("radius").requires((source) -> source.hasPermissionLevel(3)).then(
                                        CommandManager.argument("value", IntegerArgumentType.integer(-1, 128)).executes(
                                                (context) -> executeRadius(context.getSource(), IntegerArgumentType.getInteger(context, "value")))
                                )
                        ).
                        then(
                                CommandManager.literal("doShearsBreak").requires((source) -> source.hasPermissionLevel(3)).then(
                                        CommandManager.argument("value", BoolArgumentType.bool()).executes(
                                                (context) -> executeDoShearsBreak(context.getSource(), BoolArgumentType.getBool(context, "value")))
                                )
                        ).
                        then(
                                CommandManager.literal("doLeatherFix").requires((source) -> source.hasPermissionLevel(3)).then(
                                        CommandManager.argument("value", BoolArgumentType.bool()).executes(
                                                (context) -> executeDoLeatherFix(context.getSource(), BoolArgumentType.getBool(context, "value")))
                                )
                        )
        );

    }

    private static int executeColor(ServerCommandSource commandSource, Formatting color) {
        ServerPlayerEntity player = commandSource.getPlayer();
        SFramesMod.CONFIG.playerColor.put(player.getUuidAsString(), color.asString());
        SFramesMod.CONFIG.dumpJson();
        return 1;
    }

    private static int executeBaseColor(ServerCommandSource commandSource, Formatting color) {
        SFramesMod.CONFIG.outlineColor = color.asString();
        commandSource.getServer().getScoreboard().getTeam("SeamlessFrames").setColor(Formatting.byName(SFramesMod.CONFIG.outlineColor));
        SFramesMod.CONFIG.dump();
        return 1;
    }

    private static int executeRadius(ServerCommandSource commandSource, int value) {
        SFramesMod.CONFIG.radiusOfGlowing = value;
        SFramesMod.CONFIG.dump();
        return 1;
    }

    private static int executeDoShearsBreak(ServerCommandSource commandSource, boolean value) {
        SFramesMod.CONFIG.doShearsBreak = value;
        SFramesMod.CONFIG.dump();
        return 1;
    }

    private static int executeDoLeatherFix(ServerCommandSource commandSource, boolean value) {
        SFramesMod.CONFIG.fixWithLeather = value;
        SFramesMod.CONFIG.dump();
        return 1;
    }
}
