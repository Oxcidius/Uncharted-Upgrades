package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class UoHopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper, WorldlyContainer {
    private static final int SIZE = 5;
    private static final int[] SLOTS = {0, 1, 2, 3, 4};
    private final UpgradeTier tier;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int cooldown;

    public UoHopperBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, UpgradeTier tier) {
        super(type, pos, state);
        this.tier = tier;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, UoHopperBlockEntity hopper) {
        if (hopper.cooldown > 0) {
            hopper.cooldown--;
            return;
        }
        if (!state.getValue(HopperBlock.ENABLED)) {
            return;
        }

        boolean moved = hopper.eject(level, pos, state.getValue(HopperBlock.FACING), hopper.tier.hopperBatchSize);
        moved |= hopper.suck(level, pos, hopper.tier.hopperBatchSize);
        if (moved) {
            hopper.cooldown = hopper.tier.hopperCooldownTicks;
            setChanged(level, pos, state);
        }
    }

    public static void entityInside(Level level, ItemEntity itemEntity, UoHopperBlockEntity hopper) {
        if (level.isClientSide() || hopper.cooldown > 0 || itemEntity.getItem().isEmpty()) {
            return;
        }
        int moved = hopper.insertFromEntity(itemEntity, hopper.tier.hopperBatchSize);
        if (moved > 0) {
            hopper.cooldown = hopper.tier.hopperCooldownTicks;
            hopper.setChanged();
        }
    }

    private boolean eject(Level level, BlockPos pos, Direction facing, int limit) {
        Container destination = HopperBlockEntity.getContainerAt(level, pos.relative(facing));
        if (destination == null) {
            return false;
        }

        int remaining = limit;
        boolean moved = false;
        for (int slot = 0; slot < SIZE && remaining > 0; slot++) {
            ItemStack source = getItem(slot);
            if (source.isEmpty()) {
                continue;
            }
            int attempted = Math.min(remaining, source.getCount());
            ItemStack transfer = source.copyWithCount(attempted);
            ItemStack remainder = HopperBlockEntity.addItem(this, destination, transfer, facing.getOpposite());
            int transferred = attempted - remainder.getCount();
            if (transferred > 0) {
                source.shrink(transferred);
                setItem(slot, source);
                remaining -= transferred;
                moved = true;
            }
        }
        return moved;
    }

    private boolean suck(Level level, BlockPos pos, int limit) {
        int remaining = limit;
        boolean moved = false;
        Container source = HopperBlockEntity.getContainerAt(level, pos.above());
        if (source != null) {
            if (source instanceof WorldlyContainer worldly) {
                for (int slot : worldly.getSlotsForFace(Direction.DOWN)) {
                    if (remaining <= 0) {
                        break;
                    }
                    int transferred = pullFromSlot(source, slot, remaining);
                    remaining -= transferred;
                    moved |= transferred > 0;
                }
            } else {
                for (int slot = 0; slot < source.getContainerSize() && remaining > 0; slot++) {
                    int transferred = pullFromSlot(source, slot, remaining);
                    remaining -= transferred;
                    moved |= transferred > 0;
                }
            }
            return moved;
        }

        for (ItemEntity itemEntity : HopperBlockEntity.getItemsAtAndAbove(level, this)) {
            if (remaining <= 0) {
                break;
            }
            int transferred = insertFromEntity(itemEntity, remaining);
            remaining -= transferred;
            moved |= transferred > 0;
        }
        return moved;
    }

    private int pullFromSlot(Container source, int slot, int limit) {
        ItemStack sourceStack = source.getItem(slot);
        if (sourceStack.isEmpty() || !canTake(source, slot, sourceStack)) {
            return 0;
        }

        int attempted = Math.min(limit, sourceStack.getCount());
        ItemStack transfer = sourceStack.copyWithCount(attempted);
        ItemStack remainder = HopperBlockEntity.addItem(source, this, transfer, Direction.UP);
        int transferred = attempted - remainder.getCount();
        if (transferred > 0) {
            sourceStack.shrink(transferred);
            source.setItem(slot, sourceStack);
            source.setChanged();
        }
        return transferred;
    }

    private int insertFromEntity(ItemEntity itemEntity, int limit) {
        ItemStack entityStack = itemEntity.getItem();
        int attempted = Math.min(limit, entityStack.getCount());
        ItemStack transfer = entityStack.copyWithCount(attempted);
        ItemStack remainder = HopperBlockEntity.addItem(null, this, transfer, null);
        int transferred = attempted - remainder.getCount();
        if (transferred > 0) {
            entityStack.shrink(transferred);
            if (entityStack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(entityStack);
            }
        }
        return transferred;
    }

    private static boolean canTake(Container source, int slot, ItemStack stack) {
        return !(source instanceof WorldlyContainer worldly)
                || worldly.canTakeItemThroughFace(slot, stack, Direction.DOWN);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        cooldown = input.getIntOr("TransferCooldown", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("TransferCooldown", cooldown);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block." + UoUpgradeKits.MOD_ID + "." + tier.id + "_hopper");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new HopperMenu(containerId, inventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public double getLevelX() {
        return worldPosition.getX() + 0.5D;
    }

    @Override
    public double getLevelY() {
        return worldPosition.getY() + 0.5D;
    }

    @Override
    public double getLevelZ() {
        return worldPosition.getZ() + 0.5D;
    }

    @Override
    public boolean isGridAligned() {
        return true;
    }
}
