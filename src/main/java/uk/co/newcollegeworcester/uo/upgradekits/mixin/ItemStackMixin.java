package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.newcollegeworcester.uo.upgradekits.TieredBundleItem;

@Mixin(DataComponentHolder.class)
public interface ItemStackMixin {
    @Inject(
            method = "get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
            at = @At("RETURN")
    )
    private <T> void uncharted_upgrades$rememberTieredBundleCapacity(
            DataComponentType<? extends T> component,
            CallbackInfoReturnable<T> callback
    ) {
        if ((Object) this instanceof ItemStack stack
                && component == DataComponents.BUNDLE_CONTENTS
                && stack.getItem() instanceof TieredBundleItem tiered
                && callback.getReturnValue() instanceof BundleContents contents) {
            TieredBundleItem.rememberCapacity(contents, tiered.getTier().bundleCapacity);
        }
    }
}
