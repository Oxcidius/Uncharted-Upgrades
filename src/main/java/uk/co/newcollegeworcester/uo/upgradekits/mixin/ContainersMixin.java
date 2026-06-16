package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.newcollegeworcester.uo.upgradekits.UoUpgradeKits;

@Mixin(Containers.class)
public final class ContainersMixin {
    private ContainersMixin() {
    }

    @Inject(method = "dropContents(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/Container;)V", at = @At("HEAD"), cancellable = true)
    private static void uncharted_upgrades$skipUpgradeDrops(Level level, BlockPos pos, Container container, CallbackInfo callbackInfo) {
        if (UoUpgradeKits.isSuppressingContainerDrops(pos)) {
            callbackInfo.cancel();
        }
    }
}
