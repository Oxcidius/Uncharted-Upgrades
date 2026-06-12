package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.monster.Shulker;

public final class UoShulkerBlockEntity extends RandomizableContainerBlockEntity implements ExtendedScreenHandlerFactory<Integer>, WorldlyContainer {
    private enum AnimationStatus {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING
    }

    private final UpgradeTier tier;
    private final UoShulkerVariant variant;
    private NonNullList<ItemStack> items;
    private AnimationStatus animationStatus = AnimationStatus.CLOSED;
    private float progress;
    private float progressOld;
    private int openCount;

    public UoShulkerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            UpgradeTier tier,
            UoShulkerVariant variant
    ) {
        super(type, pos, state);
        this.tier = tier;
        this.variant = variant;
        this.items = NonNullList.withSize(tier.storageSlots, ItemStack.EMPTY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, UoShulkerBlockEntity shulker) {
        shulker.progressOld = shulker.progress;
        switch (shulker.animationStatus) {
            case CLOSED -> shulker.progress = 0.0F;
            case OPENING -> {
                shulker.progress = Math.min(1.0F, shulker.progress + 0.1F);
                if (shulker.progressOld == 0.0F) {
                    doNeighborUpdates(level, pos, state);
                }
                if (shulker.progress >= 1.0F) {
                    shulker.animationStatus = AnimationStatus.OPENED;
                    doNeighborUpdates(level, pos, state);
                }
                shulker.moveCollidedEntities(level, pos, state);
            }
            case OPENED -> shulker.progress = 1.0F;
            case CLOSING -> {
                shulker.progress = Math.max(0.0F, shulker.progress - 0.1F);
                if (shulker.progressOld == 1.0F) {
                    doNeighborUpdates(level, pos, state);
                }
                if (shulker.progress <= 0.0F) {
                    shulker.animationStatus = AnimationStatus.CLOSED;
                    doNeighborUpdates(level, pos, state);
                }
            }
        }
    }

    @Override
    public int getContainerSize() {
        return tier.storageSlots;
    }

    public UpgradeTier getTier() {
        return tier;
    }

    public UoShulkerVariant getVariant() {
        return variant;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block." + UoUpgradeKits.MOD_ID + "." + variant.blockId(tier));
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        NonNullList<ItemStack> resized = NonNullList.withSize(tier.storageSlots, ItemStack.EMPTY);
        for (int slot = 0; slot < Math.min(tier.storageSlots, items.size()); slot++) {
            resized.set(slot, items.get(slot));
        }
        this.items = resized;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(tier.storageSlots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new UoChestMenu(containerId, inventory, this, tier.storageSlots);
    }

    @Override
    public Integer getScreenOpeningData(ServerPlayer player) {
        return tier.storageSlots;
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (remove || user.getLivingEntity().isSpectator()) {
            return;
        }
        openCount = Math.max(0, openCount) + 1;
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, openCount);
        if (openCount == 1) {
            level.gameEvent(user.getLivingEntity(), GameEvent.CONTAINER_OPEN, worldPosition);
            level.playSound(null, worldPosition, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (remove || user.getLivingEntity().isSpectator()) {
            return;
        }
        openCount = Math.max(0, openCount - 1);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, openCount);
        if (openCount == 0) {
            level.gameEvent(user.getLivingEntity(), GameEvent.CONTAINER_CLOSE, worldPosition);
            level.playSound(null, worldPosition, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            openCount = type;
            animationStatus = type == 0 ? AnimationStatus.CLOSING : AnimationStatus.OPENING;
            return true;
        }
        return super.triggerEvent(id, type);
    }

    public float getProgress(float partialTick) {
        return Mth.lerp(partialTick, progressOld, progress);
    }

    public boolean isClosed() {
        return animationStatus == AnimationStatus.CLOSED;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return tier.storageSlotIndexes();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !(Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    public AABB getBoundingBox(BlockState state) {
        return Shulker.getProgressAabb(
                1.0F,
                state.getValue(ShulkerBoxBlock.FACING),
                0.5F * getProgress(1.0F),
                new Vec3(0.5D, 0.0D, 0.5D)
        );
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    private static void doNeighborUpdates(Level level, BlockPos pos, BlockState state) {
        state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, state.getBlock());
    }

    private void moveCollidedEntities(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(ShulkerBoxBlock.FACING);
        AABB movement = Shulker.getProgressDeltaAabb(
                1.0F,
                direction,
                progressOld,
                progress,
                pos.getBottomCenter()
        );
        for (Entity entity : level.getEntities(null, movement)) {
            if (entity.getPistonPushReaction() == PushReaction.IGNORE) {
                continue;
            }
            entity.move(
                    MoverType.SHULKER_BOX,
                    new Vec3(
                            (movement.getXsize() + 0.01D) * direction.getStepX(),
                            (movement.getYsize() + 0.01D) * direction.getStepY(),
                            (movement.getZsize() + 0.01D) * direction.getStepZ()
                    )
            );
        }
    }
}
