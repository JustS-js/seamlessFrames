package net.just_s.sframes.mixin;

import net.just_s.sframes.ItemFrameScheduledPacketAccess;
import net.just_s.sframes.SFramesMod;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

import static net.just_s.sframes.SFramesMod.generateGlowPacket;

@Mixin(BlockAttachedEntity.class)
public abstract class GlowingTickMixin implements ItemFrameScheduledPacketAccess {
    @Unique
    private int tick = 0;

    @Unique
    private int tickScheduledLeft = 0;

    @Unique
    private Set<ServerPlayerEntity> playersNearby = new HashSet<>();

    @Override
    public void sframes$shedulePacket() {
        tickScheduledLeft = 20;
    }

    @Unique
    private void tickScheduled() {
        if (this.tickScheduledLeft <= 0) {
            this.tickScheduledLeft = 0;
            return;
        }

        this.tickScheduledLeft--;

        if (this.tickScheduledLeft == 0) {
            ItemFrameEntity frame = (ItemFrameEntity) (Object) this;
            SFramesMod.sendPackets(
                    SFramesMod.getPlayersNearby(frame),
                    generateGlowPacket(frame, SFramesMod.shouldGlow(frame))
            );
        }
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void sframes$glowingTick(CallbackInfo ci) {
        BlockAttachedEntity frame = (BlockAttachedEntity) (Object) this;
        if (frame.getEntityWorld().isClient()) return;
        if (!(frame instanceof ItemFrameEntity && SFramesMod.shouldGlow((ItemFrameEntity) frame))) return;

        this.tickScheduled();

        tick++;
        if (tick % 10 != 0) return;
        tick = tick % 10;

        Set<ServerPlayerEntity> freshPlayersNearby = new HashSet<>(SFramesMod.getPlayersNearby((ItemFrameEntity) frame));

        Set<ServerPlayerEntity> playersNearbyWithoutGlow = new HashSet<>(freshPlayersNearby);
        playersNearbyWithoutGlow.removeAll(playersNearby);
        Set<ServerPlayerEntity> playersFarAway = new HashSet<>(playersNearby);
        playersFarAway.removeAll(freshPlayersNearby);
        Set<ServerPlayerEntity> difference = new HashSet<>(playersNearbyWithoutGlow);
        difference.addAll(playersFarAway);

        if (!SFramesMod.CONFIG.getData().clientSideGlowing()) {
            // glowing serverside
            frame.setGlowing(!freshPlayersNearby.isEmpty());
        } else {
            // glowing clientside
            for (ServerPlayerEntity player : difference) {
                SFramesMod.sendPacket(
                        player,
                        generateGlowPacket((ItemFrameEntity) frame, playersNearbyWithoutGlow.contains(player))
                );
            }
        }

        playersNearby = freshPlayersNearby;
    }
}
