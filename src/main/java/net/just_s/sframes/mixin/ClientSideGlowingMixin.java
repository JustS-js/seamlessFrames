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
public class ClientSideGlowingMixin {
    private String fieldName = null;

    @Inject(at=@At("TAIL"), method = "tick")
    private void inject(CallbackInfo ci) {
        try {
            if (!SFramesMod.CONFIG.clientSideGlowing) return;

            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            Team team = player.getServer().getScoreboard().getTeam("SeamlessFrames");

            clientSideGlowingTick(player, team);

        } catch (Exception e) {
            SFramesMod.LOGGER.error("SFrames error on ClientSideGlowingMixin.tick(): " + e);
        }
    }

    private void clientSideGlowingTick(ServerPlayerEntity player, Team team) throws Exception{
        // Send Glow effect
        List<Entity> entities = player.getEntityWorld().getOtherEntities(null, new Box(
                player.getBlockPos().add(SFramesMod.CONFIG.radiusOfGlowing + 1, SFramesMod.CONFIG.radiusOfGlowing + 1, SFramesMod.CONFIG.radiusOfGlowing + 1),
                player.getBlockPos().add(-1 * SFramesMod.CONFIG.radiusOfGlowing, -1 * SFramesMod.CONFIG.radiusOfGlowing, -1 * SFramesMod.CONFIG.radiusOfGlowing)
        ));
        for (Entity entity : entities) {
            if (entity instanceof ItemFrameEntity && SFramesMod.shouldGlow((ItemFrameEntity) entity)) {
                DataTracker tracker = entity.getDataTracker();

                if (fieldName == null) findObfuscatedFieldName(entity);
                Field field = entity.getClass().getField(fieldName);
                SFramesMod.LOGGER.error("got field");
                field.setAccessible(true);
                SFramesMod.LOGGER.error("set access to true");
                // changing Glowing flag on true

                TrackedData<Byte> key = (TrackedData<Byte>) field.get(entity);
                SFramesMod.LOGGER.error("got key");
                SFramesMod.LOGGER.error("is key null: " + (key == null));

                byte value = (byte) (tracker.get(key) | 1 << 6);
                SFramesMod.LOGGER.error("got value");

                tracker.set(key, value);
                SFramesMod.LOGGER.error("set tracker to true");
                SFramesMod.sendPackets(player, new EntityTrackerUpdateS2CPacket(
                        entity.getId(),
                        tracker,
                        true
                ));
                SFramesMod.LOGGER.error("sent packet");
                // changing Glowing flag back
                tracker.set((TrackedData<? super Byte>) field.get(entity), (byte) ((Byte) tracker.get((TrackedData<? extends Object>) field.get(entity)) & ~(1 << 6)));
                SFramesMod.LOGGER.error("set tracker to false");

                // Send Custom Color of frame
                if (SFramesMod.CONFIG.playerColor.containsKey(player.getEntityName())) {
                    SerializableTeam serializableTeam = new SerializableTeam(team, Formatting.byName(SFramesMod.CONFIG.playerColor.get(player.getEntityName())));

                    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeString(team.getName());
                    buf.writeByte(2);
                    serializableTeam.write(buf);

                    SFramesMod.sendPackets(player, new TeamS2CPacket(buf));
                }
            }
        }
    }

    private void findObfuscatedFieldName(Entity entity) {
        for (Field field : entity.getClass().getFields()) {
            try {
                TrackedData<Byte> x = (TrackedData<Byte>) field.get(entity);
                fieldName = field.getName();
                SFramesMod.LOGGER.info("found obfuscated name > " + fieldName);
                break;
            } catch (Exception e) {
                SFramesMod.LOGGER.warn("xxx > " + e);
            }
        }
    }
}
