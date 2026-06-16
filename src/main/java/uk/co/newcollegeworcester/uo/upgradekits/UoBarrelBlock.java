package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public final class UoBarrelBlock extends BarrelBlock {
    private final Supplier<BlockEntityType<? extends UoBarrelBlockEntity>> blockEntityType;
    private final UpgradeTier tier;

    public UoBarrelBlock(Supplier<BlockEntityType<? extends UoBarrelBlockEntity>> blockEntityType, UpgradeTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.blockEntityType = blockEntityType;
        this.tier = tier;
    }

    public UpgradeTier getTier() {
        return tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UoBarrelBlockEntity(blockEntityType.get(), pos, state, tier);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof UoBarrelBlockEntity barrel) {
            player.openMenu(barrel);
            player.awardStat(Stats.OPEN_BARREL);
            PiglinAi.angerNearbyPiglins(serverLevel, player, true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof UoBarrelBlockEntity barrel) {
            barrel.recheckOpen();
        }
    }

}
