package net.just_s.sframes.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public class ItemFrameMixin {
	@Inject(at = @At("HEAD"), method = "damage", cancellable = true)
	private void injectDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (source.getAttacker().isPlayer()) {
			PlayerEntity player = (PlayerEntity) source.getAttacker();
			ItemStack itemStackInHand = player.getInventory().getStack(player.getInventory().selectedSlot);

			ItemFrameEntity frame = ((ItemFrameEntity)(Object)this);
			if (itemStackInHand.getItem().getTranslationKey().equals("item.minecraft.shears") && !frame.isInvisible() && !frame.isGlowing()) {
				if (!player.isCreative()) {
					if (itemStackInHand.getDamage() < 237) {
						itemStackInHand.damage(1, Random.create(), player.getServer().getPlayerManager().getPlayer(player.getUuid()));
					} else {
						itemStackInHand.decrement(1);
					}

				}
				frame.getWorld().playSound(
						null,
						frame.getBlockPos(),
						SoundEvents.ENTITY_SNOW_GOLEM_SHEAR,
						SoundCategory.NEUTRAL,
						1f,
						1.5f
				);

				if (frame.getHeldItemStack().isEmpty()) {
					frame.setGlowing(true);
				} else {
					frame.setInvisible(true);
				}

				cir.setReturnValue(true);
				cir.cancel();
			}
			if (itemStackInHand.getItem().getTranslationKey().equals("item.minecraft.leather") && (frame.isInvisible() || frame.isGlowing())) {
				if (!player.isCreative()) {itemStackInHand.decrement(1);}
				frame.getWorld().playSound(
						null,
						frame.getBlockPos(),
						SoundEvents.ENTITY_ITEM_FRAME_PLACE,
						SoundCategory.NEUTRAL,
						1f,
						1.5f
				);

				if (frame.getHeldItemStack().isEmpty()) {
					frame.setGlowing(false);
				} else {
					frame.setInvisible(false);
				}

				cir.setReturnValue(true);
				cir.cancel();
			}
		}
	}

	@Inject(at = @At("RETURN"), method = "interact")
	private void injectInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		updateState();
	}

	@Inject(at = @At("RETURN"), method = "dropHeldStack")
	private void injectDropItem(Entity entity, boolean alwaysDrop, CallbackInfo ci) {
		updateState();
	}

	@Inject(at = @At("TAIL"), method = "getAsItemStack", cancellable = true)
	private void injectAsItem(CallbackInfoReturnable<ItemStack> cir) {
		ItemFrameEntity frame = ((ItemFrameEntity)(Object)this);
		if (frame.isGlowing()) {
			ItemStack item = cir.getReturnValue();
			item.setCustomName(Text.of("Невидимая рамка"));
			item.addEnchantment(Enchantments.UNBREAKING, 1);

			NbtCompound nbt = item.getOrCreateNbt();
			nbt.putBoolean("invisibleframe", true);

			cir.setReturnValue(item);
		}
	}

	private void updateState() {
		ItemFrameEntity frame = ((ItemFrameEntity)(Object)this);
		if (frame.isGlowing() && !frame.getHeldItemStack().isEmpty()) {
			frame.setGlowing(false);
			frame.setInvisible(true);
		}
		if (frame.isInvisible() && frame.getHeldItemStack().isEmpty()) {
			frame.setInvisible(false);
			frame.setGlowing(true);
		}
	}
}
