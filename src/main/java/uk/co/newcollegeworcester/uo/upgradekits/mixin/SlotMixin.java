package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import uk.co.newcollegeworcester.uo.upgradekits.MutableSlotPosition;

import net.minecraft.world.inventory.Slot;

@Mixin(Slot.class)
public abstract class SlotMixin implements MutableSlotPosition {
    @Mutable
    @Final
    @Shadow
    public int x;

    @Mutable
    @Final
    @Shadow
    public int y;

    @Override
    public void uncharted_upgrades$setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
