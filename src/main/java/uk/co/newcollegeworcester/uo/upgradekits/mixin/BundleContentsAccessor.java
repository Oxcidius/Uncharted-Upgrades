package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import com.mojang.serialization.DataResult;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BundleContents.class)
public interface BundleContentsAccessor {
    @Invoker("getWeight")
    static DataResult<Fraction> uncharted_upgrades$getWeight(ItemInstance item) {
        throw new AssertionError();
    }
}
