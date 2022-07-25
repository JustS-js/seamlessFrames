package net.just_s.sframes.mixin;

import net.just_s.sframes.SFramesMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerMixin {
    @Shadow @Final private Entity entity;

    @Inject(at = @At("HEAD"), method = "syncEntityData", cancellable = true)
    private void inject(CallbackInfo ci) {
        try {
            Entity entity = this.entity;
            if (entity instanceof ItemFrameEntity && SFramesMod.shouldGlow((ItemFrameEntity) entity)) {
                // if frame should glow clientside, it won't send "real" packets
                ci.cancel();
            }
        } catch (Exception e) {
            SFramesMod.LOGGER.error("SFrames error on EntityTrackerMixin: " + e);
        }
    }
}
