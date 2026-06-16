package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public final class TieredFurnaceBlock extends FurnaceBlock {
    private final Supplier<BlockEntityType<TieredCookingBlockEntity>> blockEntityType;
    private final UpgradeTier tier;

    public TieredFurnaceBlock(
            Supplier<BlockEntityType<TieredCookingBlockEntity>> blockEntityType,
            UpgradeTier tier,
            BlockBehaviour.Properties properties
    ) {
        super(properties);
        this.blockEntityType = blockEntityType;
        this.tier = tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TieredCookingBlockEntity(blockEntityType.get(), pos, state, tier, CookingType.FURNACE);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return createFurnaceTicker(level, type, blockEntityType.get());
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
            player.openMenu(menuProvider);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }
}
