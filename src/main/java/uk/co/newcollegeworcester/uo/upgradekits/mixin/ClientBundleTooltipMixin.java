package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.BundleContents;
import com.mojang.serialization.DataResult;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.newcollegeworcester.uo.upgradekits.TieredBundleTooltipCapacity;

@Mixin(value = ClientBundleTooltip.class, priority = 1100)
public abstract class ClientBundleTooltipMixin {
    private static final ThreadLocal<Component> TIERED_PROGRESS_TEXT = new ThreadLocal<>();

    @Shadow
    private BundleContents contents;

    @Shadow
    private static Component getProgressBarFillText(Fraction fullness) {
        throw new AssertionError();
    }

    @Redirect(
            method = "extractImage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/component/BundleContents;weight()Lcom/mojang/serialization/DataResult;"
            )
    )
    private DataResult<Fraction> uncharted_upgrades$useTieredFullness(BundleContents contents) {
        if ((Object) this instanceof TieredBundleTooltipCapacity tiered) {
            return tiered.uncharted_upgrades$fullContents().weight()
                    .map(weight -> weight.divideBy(Fraction.getFraction(tiered.uncharted_upgrades$capacity(), 1)));
        }
        return contents.weight();
    }

    @Inject(method = "extractImage", at = @At("HEAD"))
    private void uncharted_upgrades$prepareTieredItemCount(
            Font font,
            int x,
            int y,
            int width,
            int height,
            GuiGraphicsExtractor graphics,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof TieredBundleTooltipCapacity tiered
                && !tiered.uncharted_upgrades$fullContents().isEmpty()) {
            int storedUnits = tiered.uncharted_upgrades$fullContents().weight()
                    .getOrThrow()
                    .multiplyBy(Fraction.getFraction(64, 1))
                    .intValue();
            int maximumUnits = tiered.uncharted_upgrades$capacity() * 64;
            TIERED_PROGRESS_TEXT.set(Component.literal(storedUnits + "/" + maximumUnits));
        }
    }

    @Inject(method = "extractImage", at = @At("RETURN"))
    private void uncharted_upgrades$clearTieredItemCount(
            Font font,
            int x,
            int y,
            int width,
            int height,
            GuiGraphicsExtractor graphics,
            CallbackInfo ci
    ) {
        TIERED_PROGRESS_TEXT.remove();
    }

    @Redirect(
            method = "extractProgressbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientBundleTooltip;getProgressBarFillText(Lorg/apache/commons/lang3/math/Fraction;)Lnet/minecraft/network/chat/Component;"
            )
    )
    private static Component uncharted_upgrades$showTieredItemCount(Fraction fullness) {
        Component override = TIERED_PROGRESS_TEXT.get();
        return override == null ? getProgressBarFillText(fullness) : override;
    }
}
