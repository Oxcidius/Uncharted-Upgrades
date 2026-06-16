package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public final class UoHopperBlock extends HopperBlock {
    private final Supplier<BlockEntityType<? extends UoHopperBlockEntity>> blockEntityType;
    private final UpgradeTier tier;

    public UoHopperBlock(
            Supplier<BlockEntityType<? extends UoHopperBlockEntity>> blockEntityType,
            UpgradeTier tier,
            BlockBehaviour.Properties properties
    ) {
        super(properties);
        this.blockEntityType = blockEntityType;
        this.tier = tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UoHopperBlockEntity(blockEntityType.get(), pos, state, tier);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide() || type != blockEntityType.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                UoHopperBlockEntity.serverTick(tickerLevel, pos, tickerState, (UoHopperBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof UoHopperBlockEntity hopper) {
            player.openMenu(hopper);
            player.awardStat(Stats.INSPECT_HOPPER);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effects,
            boolean canTriggerEffects
    ) {
        if (entity instanceof ItemEntity itemEntity
                && level.getBlockEntity(pos) instanceof UoHopperBlockEntity hopper) {
            UoHopperBlockEntity.entityInside(level, itemEntity, hopper);
        }
    }
}
