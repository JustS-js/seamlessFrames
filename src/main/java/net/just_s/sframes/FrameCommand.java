package net.just_s.sframes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.netty.buffer.Unpooled;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
                                CommandManager.literal("baseColor").requires((source) -> source.hasPermissionLevel(3)).
                                        executes(
                                                (context) -> executeShow(context.getSource(), "baseColor", SFramesMod.CONFIG.outlineColor)
                                        ).
                                        then(
                                        CommandManager.argument("value", ColorArgumentType.color()).executes(
                                                (context) -> executeBaseColor(context.getSource(), ColorArgumentType.getColor(context, "value")))
                                )
                        ).
                        then(
                                CommandManager.literal("radius").requires((source) -> source.hasPermissionLevel(3)).
                                        executes(
                                                (context) -> executeShow(context.getSource(), "radius", SFramesMod.CONFIG.radiusOfGlowing)
                                        ).
                                        then(
                                        CommandManager.argument("value", IntegerArgumentType.integer(-1, 128)).executes(
                                                (context) -> executeRadius(context.getSource(), IntegerArgumentType.getInteger(context, "value")))
                                )
                        ).
                        then(
                                CommandManager.literal("doShearsBreak").requires((source) -> source.hasPermissionLevel(3)).
                                        executes(
                                                (context) -> executeShow(context.getSource(), "doShearsBreak", SFramesMod.CONFIG.doShearsBreak)
                                        ).
                                        then(
                                        CommandManager.argument("value", BoolArgumentType.bool()).executes(
                                                (context) -> executeDoShearsBreak(context.getSource(), BoolArgumentType.getBool(context, "value")))
                                )
                        ).
                        then(
                                CommandManager.literal("doLeatherFix").requires((source) -> source.hasPermissionLevel(3)).
                                        executes(
                                                (context) -> executeShow(context.getSource(), "doLeatherFix", SFramesMod.CONFIG.fixWithLeather)
                                        ).
                                        then(
                                        CommandManager.argument("value", BoolArgumentType.bool()).executes(
                                                (context) -> executeDoLeatherFix(context.getSource(), BoolArgumentType.getBool(context, "value")))
                                )
                        ).
                        then(
                                CommandManager.literal("clientSideGlowing").requires((source) -> source.hasPermissionLevel(3)).
                                        executes(
                                                (context) -> executeShow(context.getSource(), "clientSideGlowing", SFramesMod.CONFIG.clientSideGlowing)
                                        ).
                                        then(
                                        CommandManager.argument("value", BoolArgumentType.bool()).executes(
                                                (context) -> executeClientSideGlowing(context.getSource(), BoolArgumentType.getBool(context, "value")))
                                )
                        )
        );

    }

    private static int executeShow(ServerCommandSource commandSource, String name, Object value) {
        commandSource.getPlayer().sendMessage(Text.of("value of " + name + ": " + value));
        return 1;
    }

    private static int executeColor(ServerCommandSource commandSource, Formatting color) {
        ServerPlayerEntity player = commandSource.getPlayer();
        SFramesMod.CONFIG.playerColor.put(player.getEntityName(), color.asString());

        Team team = player.getServer().getScoreboard().getTeam("SeamlessFrames");
        SerializableTeam serializableTeam = new SerializableTeam(team, Formatting.byName(SFramesMod.CONFIG.playerColor.get(player.getEntityName())));

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(team.getName());
        buf.writeByte(2);
        serializableTeam.write(buf);

        SFramesMod.sendPackets(player, new TeamS2CPacket(buf));

        SFramesMod.CONFIG.dumpJson();
        return 1;
    }

    private static int executeBaseColor(ServerCommandSource commandSource, Formatting color) {
        SFramesMod.CONFIG.outlineColor = color.asString();
        commandSource.getServer().getScoreboard().getTeam("SeamlessFrames").setColor(Formatting.byName(SFramesMod.CONFIG.outlineColor));
        executeShow(commandSource, "baseColor", SFramesMod.CONFIG.outlineColor);
        SFramesMod.CONFIG.dump();
        return 1;
    }

    private static int executeRadius(ServerCommandSource commandSource, int value) {
        SFramesMod.CONFIG.radiusOfGlowing = value;
        executeShow(commandSource, "radius", SFramesMod.CONFIG.radiusOfGlowing);
        SFramesMod.CONFIG.dump();
        return 1;
    }

    private static int executeDoShearsBreak(ServerCommandSource commandSource, boolean value) {
        SFramesMod.CONFIG.doShearsBreak = value;
        executeShow(commandSource, "doShearsBreak", SFramesMod.CONFIG.doShearsBreak);
        SFramesMod.CONFIG.dump();
        return 1;
    }

    private static int executeDoLeatherFix(ServerCommandSource commandSource, boolean value) {
        SFramesMod.CONFIG.fixWithLeather = value;
        executeShow(commandSource, "doLeatherFix", SFramesMod.CONFIG.fixWithLeather);
        SFramesMod.CONFIG.dump();
        return 1;
    }

    private static int executeClientSideGlowing(ServerCommandSource commandSource, boolean value) {
        SFramesMod.CONFIG.clientSideGlowing = value;
        executeShow(commandSource, "clientSideGlowing", SFramesMod.CONFIG.clientSideGlowing);
        SFramesMod.CONFIG.dump();
        return 1;
    }
}
