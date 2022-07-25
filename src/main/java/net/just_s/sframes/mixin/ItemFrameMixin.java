package net.just_s.sframes.mixin;

import net.just_s.sframes.SFramesMod;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
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
		try {
			if (source.getAttacker().isPlayer()) {
				PlayerEntity player = (PlayerEntity) source.getAttacker();
				ItemStack itemStackInHand = player.getInventory().getStack(player.getInventory().selectedSlot);

				ItemFrameEntity frame = ((ItemFrameEntity)(Object)this);
				if (itemStackInHand.getItem().getTranslationKey().equals("item.minecraft.shears") && !frame.isInvisible() && frame.getScoreboardTeam() == null) {
					if (!player.isCreative() && SFramesMod.CONFIG.doShearsBreak) {
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

					SFramesMod.sendPackets((ServerPlayerEntity) player, new ParticleS2CPacket(
							ParticleTypes.CLOUD,
							false,
							frame.getX(),
							frame.getY(),
							frame.getZ(),
							0f,
							0f,
							0f,
							0.1f,
							3
					));

					Team team = frame.getWorld().getScoreboard().getTeam("SeamlessFrames");
					frame.getWorld().getScoreboard().addPlayerToTeam(frame.getEntityName(), team);

					frame.addScoreboardTag("invisibleframe");

					if (!frame.getHeldItemStack().isEmpty()) {
						frame.setInvisible(true);
					}

					cir.setReturnValue(true);
					cir.cancel();
				}
				if (itemStackInHand.getItem().getTranslationKey().equals("item.minecraft.leather") && (frame.isInvisible() || SFramesMod.shouldGlow(frame)) && SFramesMod.CONFIG.fixWithLeather) {
					if (!player.isCreative()) {itemStackInHand.decrement(1);}
					frame.getWorld().playSound(
							null,
							frame.getBlockPos(),
							SoundEvents.ENTITY_ITEM_FRAME_PLACE,
							SoundCategory.NEUTRAL,
							1f,
							1.5f
					);

					frame.setInvisible(false);

					frame.removeScoreboardTag("invisibleframe");

					SFramesMod.sendPackets((ServerPlayerEntity) player, new ParticleS2CPacket(
							ParticleTypes.CRIT,
							false,
							frame.getX(),
							frame.getY(),
							frame.getZ(),
							0.3f,
							0.3f,
							0.3f,
							0.1f,
							10
					));

					new java.util.Timer().schedule(
							new java.util.TimerTask() {
								@Override
								public void run() {
									try {
										Team team = frame.getWorld().getScoreboard().getTeam("SeamlessFrames");
										frame.getWorld().getScoreboard().removePlayerFromTeam(frame.getEntityName(), team);
									} catch (Exception e) {
										SFramesMod.LOGGER.warn(player.getEntityName() + " interacted with vanilla invisible frame. Suppressing errors...");
									}
								}
							},
							100
					);

					cir.setReturnValue(true);
					cir.cancel();
				}
			}
		} catch (Exception e) {
			SFramesMod.LOGGER.error("SFrames error on ItemFrameMixin.damage(): " + e);
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
		try {
			if (!SFramesMod.CONFIG.fixWithLeather) return;
			ItemFrameEntity frame = ((ItemFrameEntity)(Object)this);
			if (frame.getScoreboardTags().contains("invisibleframe")) {
				ItemStack item = cir.getReturnValue();
				item.setCustomName(Text.of("Невидимая рамка"));
				item.addEnchantment(Enchantments.UNBREAKING, 1);
				item.addHideFlag(ItemStack.TooltipSection.ENCHANTMENTS);

				NbtCompound nbt = item.getOrCreateNbt();
				nbt.putBoolean("invisibleframe", true);

				cir.setReturnValue(item);
			}
		} catch (Exception e) {
			SFramesMod.LOGGER.error("SFrames error on ItemFrameMixin.getAsItemStack(): " + e);
		}
	}

	private void updateState() {
		try {
			ItemFrameEntity frame = ((ItemFrameEntity)(Object)this);
			if (frame.getScoreboardTags().contains("invisibleframe")) {
				frame.setInvisible(!frame.getHeldItemStack().isEmpty());
			}
		} catch (Exception e) {
			SFramesMod.LOGGER.error("SFrames error on ItemFrameMixin.updateState(): " + e);
		}
	}
}
