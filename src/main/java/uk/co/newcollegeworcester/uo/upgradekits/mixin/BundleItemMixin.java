package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BundleItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.newcollegeworcester.uo.upgradekits.TieredBundleItem;

@Mixin(BundleItem.class)
public abstract class BundleItemMixin {
    @Inject(method = "broadcastChangesOnContainerMenu", at = @At("HEAD"), cancellable = true)
    private void uncharted_upgrades$skipInvalidModdedMenuBroadcast(Player player, CallbackInfo callback) {
        if (!((Object) this instanceof TieredBundleItem)) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null
                && menu.slots.size()
                > ((AbstractContainerMenuAccessor) menu).uncharted_upgrades$getRemoteSlots().size()) {
            callback.cancel();
        }
    }
}
