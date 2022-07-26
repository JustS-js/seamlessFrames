package net.just_s.sframes.mixin;

import io.netty.buffer.Unpooled;
import net.just_s.sframes.SFramesMod;
import net.just_s.sframes.SerializableTeam;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class CustomColorMixin {
    @Inject(at=@At("TAIL"), method = "onPlayerConnected")
    private void inject(ServerPlayerEntity player, CallbackInfo ci) {
        // Send Custom Color of frame
        if (SFramesMod.CONFIG.playerColor.containsKey(player.getEntityName())) {
            Team team = player.getServer().getScoreboard().getTeam("SeamlessFrames");
            SerializableTeam serializableTeam = new SerializableTeam(team, Formatting.byName(SFramesMod.CONFIG.playerColor.get(player.getEntityName())));

            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeString(team.getName());
            buf.writeByte(2);
            serializableTeam.write(buf);

            SFramesMod.sendPackets(player, new TeamS2CPacket(buf));
        }
    }
}
