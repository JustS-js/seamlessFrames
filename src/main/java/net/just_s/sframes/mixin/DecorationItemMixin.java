package net.just_s.sframes.mixin;

import net.just_s.sframes.SFramesMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DecorationItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecorationItem.class)
public class DecorationItemMixin {
    @Shadow
    protected boolean canPlaceOn(PlayerEntity player, Direction side, ItemStack stack, BlockPos pos) {
        return true;
    }

    @Final
    @Shadow
    private EntityType<? extends AbstractDecorationEntity> entityType;


    @Inject(at = @At("HEAD"), method = "useOnBlock", cancellable = true)
    private void inject(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        try {
            BlockPos blockPos = context.getBlockPos();
            Direction direction = context.getSide();
            BlockPos blockPos2 = blockPos.offset(direction);
            PlayerEntity playerEntity = context.getPlayer();
            ItemStack itemStack = context.getStack();

            if (playerEntity != null && this.canPlaceOn(playerEntity, direction, itemStack, blockPos2)) {
                if (this.entityType != EntityType.ITEM_FRAME && this.entityType != EntityType.GLOW_ITEM_FRAME) {
                    return;
                }
                if (!itemStack.hasNbt()) {return;}
                NbtCompound nbt = itemStack.getNbt();
                if (nbt.contains("invisibleframe")) {
                    World world = context.getWorld();
                    AbstractDecorationEntity frameEntity;
                    if (this.entityType == EntityType.ITEM_FRAME) {
                        frameEntity = new ItemFrameEntity(world, blockPos2, direction);
                    } else {
                        frameEntity = new GlowItemFrameEntity(world, blockPos2, direction);
                    }
                    EntityType.loadFromEntityNbt(world, playerEntity, frameEntity, nbt);

                    Team team = world.getScoreboard().getTeam("SeamlessFrames");
                    world.getScoreboard().addPlayerToTeam(frameEntity.getEntityName(), team);

                    frameEntity.addScoreboardTag("invisibleframe");

                    if (frameEntity.canStayAttached()) {
                        if (!world.isClient) {
                            frameEntity.onPlace();
                            world.emitGameEvent(playerEntity, GameEvent.ENTITY_PLACE, frameEntity.getPos());
                            world.spawnEntity(frameEntity);
                        }

                        itemStack.decrement(1);
                        cir.setReturnValue(ActionResult.success(world.isClient));
                    } else {
                        cir.setReturnValue(ActionResult.CONSUME);
                    }
                }
            }
        } catch (Exception e) {
            SFramesMod.LOGGER.error("SFrames error on DecorationItemMixin: " + e);
        }
    }
}
