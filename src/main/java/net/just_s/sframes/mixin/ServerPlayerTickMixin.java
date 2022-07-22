package net.just_s.sframes.mixin;

import io.netty.buffer.Unpooled;
import net.just_s.sframes.SFramesMod;
import net.just_s.sframes.SerializableTeam;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerTickMixin {
    @Inject(at=@At("TAIL"), method = "tick")
    private void inject(CallbackInfo ci) throws Exception {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Team team = player.getServer().getScoreboard().getTeam("SeamlessFrames");
        // Send Glow effect
        List<Entity> entities = player.getEntityWorld().getOtherEntities(null, new Box(
                player.getBlockPos().add(SFramesMod.CONFIG.radiusOfGlowing + 1, SFramesMod.CONFIG.radiusOfGlowing + 1, SFramesMod.CONFIG.radiusOfGlowing + 1),
                player.getBlockPos().add(-1 * SFramesMod.CONFIG.radiusOfGlowing, -1 * SFramesMod.CONFIG.radiusOfGlowing, -1 * SFramesMod.CONFIG.radiusOfGlowing)
        ));
        for (Entity entity : entities) {
            if (entity instanceof ItemFrameEntity && SFramesMod.shouldGlow((ItemFrameEntity) entity)) {
                // extracting FLAGS field to modify in s2c packet
                DataTracker tracker = entity.getDataTracker();
                Field field = entity.getClass().getField("FLAGS");
                field.setAccessible(true);
                // changing Glowing flag on true
                tracker.set((TrackedData<? super Byte>) field.get(entity), (byte) ((Byte) tracker.get((TrackedData<? extends Object>) field.get(entity)) | 1 << 6));
                SFramesMod.sendPackets(player, new EntityTrackerUpdateS2CPacket(
                        entity.getId(),
                        tracker,
                        true
                ));
                // changing Glowing flag back
                tracker.set((TrackedData<? super Byte>) field.get(entity), (byte) ((Byte) tracker.get((TrackedData<? extends Object>) field.get(entity)) & ~(1 << 6)));

                // Send Custom Color of frame
                if (SFramesMod.CONFIG.playerColor.containsKey(player.getUuidAsString())) {
                    SerializableTeam serializableTeam = new SerializableTeam(team, Formatting.byName(SFramesMod.CONFIG.playerColor.get(player.getUuidAsString())));

                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeString(team.getName());
                    buf.writeByte(2);
                    serializableTeam.write(buf);

                    SFramesMod.sendPackets(player, new TeamS2CPacket(buf));
                }
            }
        }
    }
}
