package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.newcollegeworcester.uo.upgradekits.CookingSpeedProvider;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Redirect(
            method = {"serverTick", "setItem"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getTotalCookTime(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)I")
    )
    private static int uncharted_upgrades$scaleCookTime(ServerLevel level, AbstractFurnaceBlockEntity furnace) {
        int totalCookTime = AbstractFurnaceBlockEntityInvoker.uncharted_upgrades$callGetTotalCookTime(level, furnace);
        if (furnace instanceof CookingSpeedProvider cookingSpeedProvider) {
            return uncharted_upgrades$scaleDuration(
                    totalCookTime,
                    cookingSpeedProvider.uo$getCookSpeedMultiplier()
            );
        }
        return totalCookTime;
    }

    private static int uncharted_upgrades$scaleDuration(int duration, double multiplier) {
        if (multiplier > 0.0D && multiplier != 1.0D) {
            return Math.max(1, (int) Math.ceil(duration / multiplier));
        }
        return duration;
    }
}
