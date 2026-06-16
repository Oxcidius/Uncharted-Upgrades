package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.newcollegeworcester.uo.upgradekits.TieredBundleClientTooltip;
import uk.co.newcollegeworcester.uo.upgradekits.TieredBundleTooltip;

@Mixin(ClientTooltipComponent.class)
public interface ClientTooltipComponentMixin {
    @Inject(method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;", at = @At("HEAD"), cancellable = true)
    private static void uncharted_upgrades$createTieredBundleTooltip(
            TooltipComponent component,
            CallbackInfoReturnable<ClientTooltipComponent> callback
    ) {
        if (component instanceof TieredBundleTooltip tooltip) {
            callback.setReturnValue(new TieredBundleClientTooltip(tooltip));
        }
    }
}
