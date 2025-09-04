package net.just_s.sframes.mixin;

import net.just_s.sframes.SFramesMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecorationItem.class)
public class DecorationItemMixin {
    @Unique
    private NbtCompound customData;

    @Inject(at = @At("HEAD"), method = "useOnBlock")
    private void sframes$storeNbt(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack itemStack = context.getStack();
        NbtComponent nbtComponent = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        customData = nbtComponent.copyNbt();
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"), method = "useOnBlock")
    private boolean sframes$addFrameToTeamOnSpawn(World world, Entity abstractDecorationEntity) {
        if (abstractDecorationEntity instanceof ItemFrameEntity && customData.getBoolean("invisibleframe").orElse(false)) {
            SFramesMod.addFrameToTeam((ItemFrameEntity) abstractDecorationEntity);
        }

        return world.spawnEntity(abstractDecorationEntity);
    }
}
