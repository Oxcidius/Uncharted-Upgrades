package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;

public class CopperBlastFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    public CopperBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(UoUpgradeKits.COPPER_BLAST_FURNACE_BLOCK_ENTITY, pos, state, RecipeType.BLASTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.uo_upgrade_kits.copper_blast_furnace");
    }

    @Override
    protected int getBurnDuration(FuelValues fuelValues, ItemStack stack) {
        return super.getBurnDuration(fuelValues, stack) / 2;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new BlastFurnaceMenu(containerId, inventory, this, this.dataAccess);
    }
}
