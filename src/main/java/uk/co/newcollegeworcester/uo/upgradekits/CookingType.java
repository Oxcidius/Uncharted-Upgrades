package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum CookingType {
    FURNACE("furnace", RecipeType.SMELTING, 1.0D),
    SMOKER("smoker", RecipeType.SMOKING, 2.0D),
    BLAST_FURNACE("blast_furnace", RecipeType.BLASTING, 2.0D);

    public final String id;
    public final RecipeType<?> recipeType;
    public final double fuelSpeedMultiplier;

    CookingType(String id, RecipeType<?> recipeType, double fuelSpeedMultiplier) {
        this.id = id;
        this.recipeType = recipeType;
        this.fuelSpeedMultiplier = fuelSpeedMultiplier;
    }

    public Block vanillaBlock() {
        return switch (this) {
            case FURNACE -> Blocks.FURNACE;
            case SMOKER -> Blocks.SMOKER;
            case BLAST_FURNACE -> Blocks.BLAST_FURNACE;
        };
    }
}
