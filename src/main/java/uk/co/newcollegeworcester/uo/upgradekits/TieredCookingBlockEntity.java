package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;

public final class TieredCookingBlockEntity extends AbstractFurnaceBlockEntity implements CookingSpeedProvider {
    private final UpgradeTier tier;
    private final CookingType cookingType;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public TieredCookingBlockEntity(
            BlockEntityType<?> blockEntityType,
            BlockPos pos,
            BlockState state,
            UpgradeTier tier,
            CookingType cookingType
    ) {
        super(blockEntityType, pos, state, (RecipeType) cookingType.recipeType);
        this.tier = tier;
        this.cookingType = cookingType;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(
                "block." + UoUpgradeKits.MOD_ID + "." + tier.id + "_" + cookingType.id
        );
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return switch (cookingType) {
            case FURNACE -> new FurnaceMenu(containerId, inventory, this, dataAccess);
            case SMOKER -> new SmokerMenu(containerId, inventory, this, dataAccess);
            case BLAST_FURNACE -> new BlastFurnaceMenu(containerId, inventory, this, dataAccess);
        };
    }

    @Override
    public double uo$getCookSpeedMultiplier() {
        return tier.cookingSpeedMultiplier;
    }

    @Override
    protected int getBurnDuration(FuelValues fuelValues, ItemStack fuel) {
        int vanillaDuration = super.getBurnDuration(fuelValues, fuel);
        double divisor = tier.cookingSpeedMultiplier * cookingType.fuelSpeedMultiplier;
        return vanillaDuration <= 0 ? 0 : Math.max(1, (int) Math.ceil(vanillaDuration / divisor));
    }
}
