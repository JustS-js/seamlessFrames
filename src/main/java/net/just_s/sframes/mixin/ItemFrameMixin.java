package net.just_s.sframes.mixin;

import net.just_s.sframes.ItemFrameScheduledPacketAccess;
import net.just_s.sframes.SFramesMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ItemFrameEntity.class)
public class ItemFrameMixin {
	@Inject(at = @At("HEAD"), method = "damage", cancellable = true)
	private void sframes$onItemFrameDamageWithTool(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		Entity attacker = source.getAttacker();
		if (attacker == null || !attacker.isPlayer() || attacker.getWorld().isClient()) return;

		ServerPlayerEntity player = (ServerPlayerEntity) attacker;
		ItemStack itemStackInHand = player.getInventory().getStack(player.getInventory().getSelectedSlot());

		if (itemStackInHand.isOf(Items.SHEARS) && this.applyShears(itemStackInHand, player)) {
			cir.setReturnValue(true);
		} else if (itemStackInHand.isOf(Items.LEATHER) && this.applyLeather(itemStackInHand, player)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(at = @At("RETURN"), method = "interact")
	private void sframes$onPlacingItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		updateState();
	}

	@Inject(at = @At("RETURN"), method = "dropHeldStack")
	private void sframes$onDroppingHoldingItem(ServerWorld world, Entity entity, boolean dropSelf, CallbackInfo ci) {
		updateState();
	}

	@Inject(at = @At("TAIL"), method = "getAsItemStack", cancellable = true)
	private void sframes$onDroppingAsItem(CallbackInfoReturnable<ItemStack> cir) {
		// If players can't fix item frames with leather, we should drop vanilla frames
		if (!SFramesMod.CONFIG.getData().fixWithLeather()) return;

		ItemFrameEntity itemFrame = ((ItemFrameEntity)(Object)this);
		if (!SFramesMod.isFrameInTeam(itemFrame)) return;

		ItemStack item = cir.getReturnValue();
		item.set(DataComponentTypes.ITEM_NAME, Text.of(SFramesMod.CONFIG.getData().invisibleItemFrameName()));
		item.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

		NbtCompound nbt = new NbtCompound();
		nbt.putBoolean("invisibleframe", true);
		item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

		cir.setReturnValue(item);
	}

	@Unique
	private void updateState() {
		ItemFrameEntity itemFrame = ((ItemFrameEntity)(Object)this);
		if (!SFramesMod.isFrameInTeam(itemFrame)) return;

		itemFrame.setInvisible(!itemFrame.getHeldItemStack().isEmpty());

		if (SFramesMod.CONFIG.getData().clientSideGlowing()) {
			((ItemFrameScheduledPacketAccess)itemFrame).sframes$shedulePacket();
		} else {
			itemFrame.setGlowing(SFramesMod.shouldGlow(itemFrame));
		}
	}

	@Unique
	private boolean applyShears(ItemStack itemStackInHand, ServerPlayerEntity player) {
		ItemFrameEntity itemFrame = (ItemFrameEntity)(Object)this;

		if (SFramesMod.isFrameInTeam(itemFrame)) return false;

		SFramesMod.addFrameToTeam(itemFrame);

		itemFrame.getWorld().playSound(
				null,
				itemFrame.getBlockPos(),
				SoundEvents.ENTITY_SNOW_GOLEM_SHEAR,
				SoundCategory.NEUTRAL,
				1f,
				1.5f
		);

		SFramesMod.sendPacket(player, new ParticleS2CPacket(
				ParticleTypes.CLOUD,
				false,
				true,
				itemFrame.getX(),
				itemFrame.getY(),
				itemFrame.getZ(),
				0f,
				0f,
				0f,
				0.1f,
				3
		));

		if (SFramesMod.CONFIG.getData().doShearsBreak()) {
			itemStackInHand.damage(1, player, EquipmentSlot.MAINHAND);
		}

		itemFrame.setGlowing(!SFramesMod.CONFIG.getData().clientSideGlowing() && SFramesMod.shouldGlow(itemFrame));
		itemFrame.setInvisible(!itemFrame.getHeldItemStack().isEmpty());
		SFramesMod.sendPackets(SFramesMod.getPlayersNearby(itemFrame), SFramesMod.generateGlowPacket(itemFrame, false));

		return true;
	}

	@Unique
	private boolean applyLeather(ItemStack itemStackInHand, ServerPlayerEntity player) {
		ItemFrameEntity itemFrame = (ItemFrameEntity)(Object)this;

		if (!SFramesMod.CONFIG.getData().fixWithLeather()) return false;
		if (!SFramesMod.isFrameInTeam(itemFrame)) return false;

		SFramesMod.removeFrameFromTeam(itemFrame);

		itemFrame.getWorld().playSound(
				null,
				itemFrame.getBlockPos(),
				SoundEvents.ENTITY_ITEM_FRAME_PLACE,
				SoundCategory.NEUTRAL,
				1f,
				1.5f
		);

		itemStackInHand.decrementUnlessCreative(1, player);

		itemFrame.setGlowing(false);
		itemFrame.setInvisible(false);
		SFramesMod.sendPackets(SFramesMod.getPlayersNearby(itemFrame), SFramesMod.generateGlowPacket(itemFrame, false));

		return true;
	}
}
