package net.just_s.sframes.mixin;

import net.just_s.sframes.SFramesMod;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public class ItemFrameMixin {
	@Inject(at = @At("HEAD"), method = "damage", cancellable = true)
	private void inject(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (source.getAttacker().isPlayer()) {
			PlayerEntity player = (PlayerEntity) source.getAttacker();
			ItemStack itemStack = player.getInventory().getStack(player.getInventory().selectedSlot);
			if (itemStack.getItem().getTranslationKey().equals("item.minecraft.shears") && !((ItemFrameEntity)(Object)this).isInvisible()) {
				ItemFrameEntity frame = ((ItemFrameEntity)(Object)this);
				if (!player.isCreative()) {
					if (itemStack.getDamage() < 237) {
						itemStack.damage(1, Random.create(), player.getServer().getPlayerManager().getPlayer(player.getUuid()));
					} else {
						itemStack.decrement(1);
					}

				}
				frame.getWorld().playSound(
						null,
						frame.getBlockPos(),
						SoundEvents.ENTITY_ITEM_FRAME_PLACE,
						SoundCategory.NEUTRAL,
						1f,
						1f
				);
				frame.setInvisible(true);

				cir.setReturnValue(true);
				cir.cancel();
				return;
			}
		}
	}
}
