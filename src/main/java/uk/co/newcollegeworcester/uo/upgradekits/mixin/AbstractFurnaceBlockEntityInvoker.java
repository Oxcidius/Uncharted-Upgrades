package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityInvoker {
    @Invoker("getTotalCookTime")
    static int uncharted_upgrades$callGetTotalCookTime(ServerLevel level, AbstractFurnaceBlockEntity furnace) {
        throw new AssertionError();
    }
}
