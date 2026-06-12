package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.newcollegeworcester.uo.upgradekits.TieredBundleItem;

@Mixin(BundleMouseActions.class)
public abstract class BundleMouseActionsMixin {
    @Redirect(
            method = {
                    "onMouseScrolled",
                    "toggleSelectedBundleItem"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BundleItem;getNumberOfItemsToShow(Lnet/minecraft/world/item/ItemStack;)I"
            )
    )
    private int uncharted_upgrades$selectFromAllTieredBundleItems(ItemStack stack) {
        if (stack.getItem() instanceof TieredBundleItem) {
            return stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).size();
        }
        return BundleItem.getNumberOfItemsToShow(stack);
    }
}
