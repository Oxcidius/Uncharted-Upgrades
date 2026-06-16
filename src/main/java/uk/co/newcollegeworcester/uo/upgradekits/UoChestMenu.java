package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class UoChestMenu extends AbstractContainerMenu {
    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 6;
    private static final int PLAYER_HOTBAR_SLOTS = 9;
    private static final int PLAYER_INVENTORY_SLOTS = 36;
    private static final int HIDDEN_Y = -10000;
    private final Container container;
    private final Inventory playerInventory;
    private final int storageSlots;
    private final int rows;
    private final int playerInventoryY;

    public UoChestMenu(int containerId, Inventory playerInventory, Integer storageSlots) {
        this(containerId, playerInventory, new SimpleContainer(storageSlots), storageSlots);
    }

    public UoChestMenu(int containerId, Inventory playerInventory, Container container, int storageSlots) {
        super(UoRegistries.chestMenu(), containerId);
        this.container = container;
        this.playerInventory = playerInventory;
        this.storageSlots = storageSlots;
        this.rows = (int) Math.ceil(storageSlots / (double) COLUMNS);
        this.playerInventoryY = 31 + getVisibleRows() * 18;

        container.startOpen(playerInventory.player);
        for (int slot = 0; slot < storageSlots; slot++) {
            int row = slot / COLUMNS;
            int column = slot % COLUMNS;
            Slot storageSlot = container instanceof UoShulkerBlockEntity
                    ? new ShulkerBoxSlot(container, slot, 8 + column * 18, 18 + row * 18)
                    : new Slot(container, slot, 8 + column * 18, 18 + row * 18);
            addSlot(storageSlot);
        }
        addStandardInventorySlots(playerInventory, 8, playerInventoryY);
        setScrollRow(0);
    }

    public int getStorageSlots() {
        return storageSlots;
    }

    public boolean containsContainer(Container candidate) {
        return container == candidate
                || container instanceof CompoundContainer compound && compound.contains(candidate);
    }

    public int getRows() {
        return rows;
    }

    public int getVisibleRows() {
        return Math.min(VISIBLE_ROWS, rows);
    }

    public int getPlayerInventoryY() {
        return playerInventoryY;
    }

    public int getMaxScrollRow() {
        return Math.max(0, rows - getVisibleRows());
    }

    public void setScrollRow(int rowOffset) {
        setScrollRow(rowOffset, 0);
    }

    public void setScrollRow(int rowOffset, int headerOffset) {
        int clamped = Math.max(0, Math.min(rowOffset, getMaxScrollRow()));
        for (int slotIndex = 0; slotIndex < storageSlots; slotIndex++) {
            Slot slot = slots.get(slotIndex);
            int row = slotIndex / COLUMNS;
            int column = slotIndex % COLUMNS;
            if (row >= clamped && row < clamped + getVisibleRows()) {
                ((MutableSlotPosition) slot).uncharted_upgrades$setPosition(8 + column * 18, 18 + headerOffset + (row - clamped) * 18);
            } else {
                ((MutableSlotPosition) slot).uncharted_upgrades$setPosition(8 + column * 18, HIDDEN_Y);
            }
        }
        setPlayerInventoryOffset(headerOffset);
    }

    void setPlayerInventoryOffset(int headerOffset) {
        for (int index = storageSlots; index < slots.size(); index++) {
            Slot slot = slots.get(index);
            int containerSlot = slot.getContainerSlot();
            int x;
            int y;
            if (containerSlot < PLAYER_HOTBAR_SLOTS) {
                x = 8 + containerSlot * 18;
                y = playerInventoryY + 58 + headerOffset;
            } else {
                int mainSlot = containerSlot - PLAYER_HOTBAR_SLOTS;
                x = 8 + (mainSlot % COLUMNS) * 18;
                y = playerInventoryY + (mainSlot / COLUMNS) * 18 + headerOffset;
            }
            ((MutableSlotPosition) slot).uncharted_upgrades$setPosition(x, y);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < storageSlots) {
            if (!moveItemStackTo(original, storageSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (container instanceof UoShulkerBlockEntity
                    && Block.byItem(original.getItem()) instanceof ShulkerBoxBlock) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(original, 0, storageSlots, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    public void handleButtonAction(Player player, int action) {
        if (action == UoChestActionPayload.MOVE_ALL_TO_CHEST) {
            moveAllToChest(player);
        } else if (action == UoChestActionPayload.MOVE_ALL_FROM_CHEST) {
            moveAllFromChest(player);
        }
    }

    private void moveAllToChest(Player player) {
        for (int index = storageSlots; index < slots.size(); index++) {
            Slot slot = slots.get(index);
            if (isPlayerMainInventorySlot(slot)) {
                moveSlot(index, UoChestMenu::isChestSlot);
            }
        }
    }

    private void moveAllFromChest(Player player) {
        for (int index = 0; index < storageSlots; index++) {
            moveSlot(index, this::isPlayerMainInventorySlot);
        }
    }

    private void moveSlot(int slotIndex, SlotTarget target) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return;
        }

        ItemStack original = slot.getItem();
        if (!moveItemToTargetSlots(original, target)) {
            return;
        }

        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
    }

    private boolean moveItemToTargetSlots(ItemStack stack, SlotTarget target) {
        boolean moved = false;
        for (int index = 0; index < slots.size() && !stack.isEmpty(); index++) {
            Slot targetSlot = slots.get(index);
            if (!target.matches(targetSlot) || !targetSlot.hasItem() || !targetSlot.mayPlace(stack)) {
                continue;
            }

            ItemStack existing = targetSlot.getItem();
            if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                continue;
            }

            int maxStackSize = Math.min(targetSlot.getMaxStackSize(existing), existing.getMaxStackSize());
            int transferable = Math.min(stack.getCount(), maxStackSize - existing.getCount());
            if (transferable <= 0) {
                continue;
            }

            existing.grow(transferable);
            stack.shrink(transferable);
            targetSlot.setChanged();
            moved = true;
        }

        for (int index = 0; index < slots.size() && !stack.isEmpty(); index++) {
            Slot targetSlot = slots.get(index);
            if (!target.matches(targetSlot) || targetSlot.hasItem() || !targetSlot.mayPlace(stack)) {
                continue;
            }

            int transferable = Math.min(stack.getCount(), targetSlot.getMaxStackSize(stack));
            ItemStack inserted = stack.copyWithCount(transferable);
            targetSlot.setByPlayer(inserted);
            stack.shrink(transferable);
            moved = true;
        }

        return moved;
    }

    private static boolean isChestSlot(Slot slot) {
        return slot.container != null && !(slot.container instanceof Inventory);
    }

    private boolean isPlayerMainInventorySlot(Slot slot) {
        int containerSlot = slot.getContainerSlot();
        return slot.container == playerInventory
            && containerSlot >= PLAYER_HOTBAR_SLOTS
            && containerSlot < PLAYER_INVENTORY_SLOTS;
    }

    @FunctionalInterface
    private interface SlotTarget {
        boolean matches(Slot slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
