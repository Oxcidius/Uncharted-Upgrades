package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

public final class UoBarrelBlockEntity extends RandomizableContainerBlockEntity implements ExtendedMenuProvider<Integer> {
    private final UpgradeTier tier;
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            UoBarrelBlockEntity.this.updateBlockState(state, true);
            UoBarrelBlockEntity.this.playSound(state, SoundEvents.BARREL_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            UoBarrelBlockEntity.this.updateBlockState(state, false);
            UoBarrelBlockEntity.this.playSound(state, SoundEvents.BARREL_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previousOpenCount, int openCount) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof UoChestMenu menu && menu.containsContainer(UoBarrelBlockEntity.this);
        }
    };
    private NonNullList<ItemStack> items;

    public UoBarrelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, UpgradeTier tier) {
        super(type, pos, state);
        this.tier = tier;
        this.items = NonNullList.withSize(tier.storageSlots, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return tier.storageSlots;
    }

    public UpgradeTier getTier() {
        return tier;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block." + UoUpgradeKits.MOD_ID + "." + tier.id + "_barrel");
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
        LivingEntity livingEntity = user.getLivingEntity();
        if (!remove && !livingEntity.isSpectator()) {
            openersCounter.incrementOpeners(livingEntity, getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        LivingEntity livingEntity = user.getLivingEntity();
        if (!remove && !livingEntity.isSpectator()) {
            openersCounter.decrementOpeners(livingEntity, getLevel(), getBlockPos(), getBlockState());
        }
    }

    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return openersCounter.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
    }

    public void recheckOpen() {
        if (!remove) {
            openersCounter.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
        }
    }

    private void updateBlockState(BlockState state, boolean open) {
        level.setBlock(getBlockPos(), state.setValue(BarrelBlock.OPEN, open), 3);
    }

    private void playSound(BlockState state, SoundEvent soundEvent) {
        Direction direction = state.getValue(BarrelBlock.FACING);
        double x = worldPosition.getX() + 0.5D + direction.getStepX() / 2.0D;
        double y = worldPosition.getY() + 0.5D + direction.getStepY() / 2.0D;
        double z = worldPosition.getZ() + 0.5D + direction.getStepZ() / 2.0D;
        level.playSound(null, x, y, z, soundEvent, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
