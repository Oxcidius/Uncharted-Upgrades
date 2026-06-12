package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.newcollegeworcester.uo.upgradekits.TieredBundleTooltipCapacity;

@Mixin(value = ClientBundleTooltip.class, priority = 1100)
public abstract class ClientBundleTooltipMixin {
    @Shadow
    private BundleContents contents;

    @Redirect(
            method = {
                    "getProgressBarFill",
                    "getProgressBarTexture",
                    "getProgressBarFillText"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/component/BundleContents;weight()Lorg/apache/commons/lang3/math/Fraction;"
            )
    )
    private Fraction uncharted_upgrades$useTieredFullness(BundleContents contents) {
        if ((Object) this instanceof TieredBundleTooltipCapacity tiered) {
            return tiered.uncharted_upgrades$fullContents().weight()
                    .divideBy(Fraction.getFraction(tiered.uncharted_upgrades$capacity(), 1));
        }
        return contents.weight();
    }

    @Inject(method = "getProgressBarFillText", at = @At("RETURN"), cancellable = true)
    private void uncharted_upgrades$showTieredItemCount(CallbackInfoReturnable<Component> cir) {
        if ((Object) this instanceof TieredBundleTooltipCapacity tiered
                && !tiered.uncharted_upgrades$fullContents().isEmpty()) {
            int storedUnits = tiered.uncharted_upgrades$fullContents().weight()
                    .multiplyBy(Fraction.getFraction(64, 1))
                    .intValue();
            int maximumUnits = tiered.uncharted_upgrades$capacity() * 64;
            cir.setReturnValue(Component.literal(storedUnits + "/" + maximumUnits));
        }
    }
}
