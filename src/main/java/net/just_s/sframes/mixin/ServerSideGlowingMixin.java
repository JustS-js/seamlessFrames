package net.just_s.sframes.mixin;

import io.netty.buffer.Unpooled;
import net.just_s.sframes.SFramesMod;
import net.just_s.sframes.SerializableTeam;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractDecorationEntity.class)
public abstract class ServerSideGlowingMixin {
    @Inject(at = @At("HEAD"), method = "tick")
    private void inject(CallbackInfo ci) {
        try {
            if (SFramesMod.CONFIG.clientSideGlowing) return;
            AbstractDecorationEntity frame = (AbstractDecorationEntity) (Object) this;
            if (!(frame instanceof ItemFrameEntity && SFramesMod.shouldGlow((ItemFrameEntity) frame))) return;

            List<Entity> entities = frame.getEntityWorld().getOtherEntities(null, new Box(
                    frame.getBlockPos().add(SFramesMod.CONFIG.radiusOfGlowing + 1, SFramesMod.CONFIG.radiusOfGlowing + 1, SFramesMod.CONFIG.radiusOfGlowing + 1),
                    frame.getBlockPos().add(-1 * SFramesMod.CONFIG.radiusOfGlowing, -1 * SFramesMod.CONFIG.radiusOfGlowing, -1 * SFramesMod.CONFIG.radiusOfGlowing)
            ));

            boolean NoPlayerNearby = true;
            Team team = frame.getServer().getScoreboard().getTeam("SeamlessFrames");
            for (Entity entity : entities) {
                if (entity instanceof ServerPlayerEntity) {
                    if (!frame.isGlowing()) {
                        frame.setGlowing(true);
                    }
                    NoPlayerNearby = false;
                    // Send Custom Color of frame
                    if (SFramesMod.CONFIG.playerColor.containsKey(entity.getEntityName())) {
                        SerializableTeam serializableTeam = new SerializableTeam(team, Formatting.byName(SFramesMod.CONFIG.playerColor.get(entity.getEntityName())));

                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeString(team.getName());
                        buf.writeByte(2);
                        serializableTeam.write(buf);

                        SFramesMod.sendPackets((ServerPlayerEntity) entity, new TeamS2CPacket(buf));
                    }
                }
            }

            if (frame.isGlowing() && NoPlayerNearby) {
                frame.setGlowing(false);
            }
        } catch (Exception e) {
            SFramesMod.LOGGER.error("SFrames error on ServerSideGlowingMixin.tick(): " + e);
        }
    }
}
