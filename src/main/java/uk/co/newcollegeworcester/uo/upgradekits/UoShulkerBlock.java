package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

public final class UoShulkerBlock extends ShulkerBoxBlock {
    private final Supplier<BlockEntityType<? extends UoShulkerBlockEntity>> blockEntityType;
    private final UpgradeTier tier;
    private final UoShulkerVariant variant;

    public UoShulkerBlock(
            Supplier<BlockEntityType<? extends UoShulkerBlockEntity>> blockEntityType,
            UpgradeTier tier,
            UoShulkerVariant variant,
            BlockBehaviour.Properties properties
    ) {
        super(variant.color, properties);
        this.blockEntityType = blockEntityType;
        this.tier = tier;
        this.variant = variant;
    }

    public UpgradeTier getTier() {
        return tier;
    }

    public UoShulkerVariant getVariant() {
        return variant;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UoShulkerBlockEntity(blockEntityType.get(), pos, state, tier, variant);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != blockEntityType.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                UoShulkerBlockEntity.tick(tickerLevel, pos, tickerState, (UoShulkerBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof UoShulkerBlockEntity shulker && canOpen(state, level, pos, shulker)) {
            player.openMenu(shulker);
            player.awardStat(Stats.OPEN_SHULKER_BOX);
            if (level instanceof ServerLevel serverLevel) {
                PiglinAi.angerNearbyPiglins(serverLevel, player, true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && player.preventsBlockDrops()
                && level.getBlockEntity(pos) instanceof UoShulkerBlockEntity shulker
                && !shulker.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(shulker.collectComponents());
            ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (!UoUpgradeKits.isSuppressingContainerDrops(pos)) {
            super.affectNeighborsAfterRemoval(state, level, pos, moved);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof UoShulkerBlockEntity shulker) {
            return Shapes.create(shulker.getBoundingBox(state));
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof UoShulkerBlockEntity shulker && !shulker.isClosed()) {
            return SHAPES_OPEN_SUPPORT.get(state.getValue(FACING).getOpposite());
        }
        return Shapes.block();
    }

    private static boolean canOpen(BlockState state, Level level, BlockPos pos, UoShulkerBlockEntity shulker) {
        if (!shulker.isClosed()) {
            return true;
        }
        AABB openingBox = Shulker.getProgressDeltaAabb(
                        1.0F,
                        state.getValue(FACING),
                        0.0F,
                        0.5F,
                        new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)
                )
                .deflate(1.0E-6D);
        return level.noCollision(openingBox);
    }
}
