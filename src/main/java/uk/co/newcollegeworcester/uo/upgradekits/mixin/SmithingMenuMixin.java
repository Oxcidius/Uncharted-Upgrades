package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.newcollegeworcester.uo.upgradekits.UoAdvancementService;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void uncharted_upgrades$trackConversionKitUse(
            Player player,
            ItemStack result,
        CallbackInfo callbackInfo
    ) {
        Container inputs = ((ItemCombinerMenuAccessor) this).uncharted_upgrades$getInputSlots();
        UoAdvancementService.awardSmithingUpgrade(
                player,
                inputs.getItem(SmithingMenu.ADDITIONAL_SLOT),
                result
        );
    }
}
