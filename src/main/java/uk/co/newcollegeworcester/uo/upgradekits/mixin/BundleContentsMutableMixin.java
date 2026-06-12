package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.newcollegeworcester.uo.upgradekits.TieredBundleItem;

import java.util.List;

@Mixin(targets = "net.minecraft.world.item.component.BundleContents$Mutable")
public abstract class BundleContentsMutableMixin {
    @Unique
    private Fraction uncharted_upgrades$capacity = Fraction.ONE;

    @Shadow
    private List<ItemStack> items;

    @Shadow
    private Fraction weight;

    @Shadow
    private int getMaxAmountToAdd(ItemStack stack) {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void uncharted_upgrades$captureTieredCapacity(BundleContents contents, CallbackInfo callback) {
        uncharted_upgrades$capacity = TieredBundleItem.consumeCapacity(contents);
    }

    @Redirect(
            method = "getMaxAmountToAdd",
            at = @At(
                    value = "FIELD",
                    target = "Lorg/apache/commons/lang3/math/Fraction;ONE:Lorg/apache/commons/lang3/math/Fraction;",
                    opcode = Opcodes.GETSTATIC
            )
    )
    private Fraction uncharted_upgrades$useTierCapacity() {
        Fraction activeCapacity = TieredBundleItem.activeCapacity();
        return activeCapacity.compareTo(uncharted_upgrades$capacity) >= 0
                ? activeCapacity
                : uncharted_upgrades$capacity;
    }

    @Inject(method = "tryInsert", at = @At("HEAD"), cancellable = true)
    private void uncharted_upgrades$insertIntoTieredBundle(
            ItemStack source,
            CallbackInfoReturnable<Integer> callback
    ) {
        Fraction activeCapacity = TieredBundleItem.activeCapacity();
        Fraction capacity = activeCapacity.compareTo(uncharted_upgrades$capacity) >= 0
                ? activeCapacity
                : uncharted_upgrades$capacity;
        if (capacity.compareTo(Fraction.ONE) <= 0) {
            return;
        }

        if (source.getItem() instanceof BundleItem || !BundleContents.canItemBeInBundle(source)) {
            callback.setReturnValue(0);
            return;
        }

        int amount = Math.min(source.getCount(), getMaxAmountToAdd(source));
        if (amount == 0) {
            callback.setReturnValue(0);
            return;
        }

        Fraction itemWeight = BundleContentsAccessor.uncharted_upgrades$getWeight(source);
        this.weight = this.weight.add(itemWeight.multiplyBy(Fraction.getFraction(amount, 1)));

        int remaining = amount;
        for (int index = 0; index < this.items.size() && remaining > 0; index++) {
            ItemStack existing = this.items.get(index);
            if (!existing.isStackable() || !ItemStack.isSameItemSameComponents(existing, source)) {
                continue;
            }

            int moved = Math.min(remaining, existing.getMaxStackSize() - existing.getCount());
            if (moved <= 0) {
                continue;
            }

            this.items.remove(index);
            this.items.add(0, existing.copyWithCount(existing.getCount() + moved));
            source.shrink(moved);
            remaining -= moved;
        }

        while (remaining > 0) {
            int moved = Math.min(remaining, source.getMaxStackSize());
            this.items.add(0, source.split(moved));
            remaining -= moved;
        }

        callback.setReturnValue(amount);
    }
}
