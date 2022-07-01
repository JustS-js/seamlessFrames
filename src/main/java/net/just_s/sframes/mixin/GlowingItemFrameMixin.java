package net.just_s.sframes.mixin;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlowItemFrameEntity.class)
public class GlowingItemFrameMixin {
    @Inject(at = @At("TAIL"), method = "getAsItemStack", cancellable = true)
    private void injectAsItem(CallbackInfoReturnable<ItemStack> cir) {
        ItemFrameEntity frame = ((ItemFrameEntity)(Object)this);
        if (frame.isGlowing()) {
            ItemStack item = cir.getReturnValue();
            item.setCustomName(Text.of("Невидимая светящаяся рамка"));
            item.addEnchantment(Enchantments.UNBREAKING, 1);

            NbtCompound nbt = item.getOrCreateNbt();
            nbt.putBoolean("invisibleframe", true);

            cir.setReturnValue(item);
        }
    }
}
