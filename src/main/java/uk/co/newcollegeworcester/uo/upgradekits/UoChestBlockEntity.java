package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

public final class UoChestBlockEntity extends ChestBlockEntity implements ExtendedScreenHandlerFactory<Integer> {
    private final UpgradeTier tier;
    private final ContainerOpenersCounter uoOpenersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            playChestSound(level, pos, state, SoundEvents.CHEST_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            playChestSound(level, pos, state, SoundEvents.CHEST_CLOSE);
        }

        @Override
        protected void openerCountChanged(
                Level level,
                BlockPos pos,
                BlockState state,
                int previousOpenCount,
                int openCount
        ) {
            UoChestBlockEntity.this.signalOpenCount(level, pos, state, previousOpenCount, openCount);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof UoChestMenu menu
                    && menu.containsContainer(UoChestBlockEntity.this);
        }
    };
    private NonNullList<ItemStack> items;

    public UoChestBlockEntity(BlockEntityType<? extends ChestBlockEntity> type, BlockPos pos, BlockState state, UpgradeTier tier) {
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
        return Component.translatable("block." + UoUpgradeKits.MOD_ID + "." + tier.id + "_chest");
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
            uoOpenersCounter.incrementOpeners(
                    livingEntity,
                    getLevel(),
                    getBlockPos(),
                    getBlockState(),
                    user.getContainerInteractionRange()
            );
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        LivingEntity livingEntity = user.getLivingEntity();
        if (!remove && !livingEntity.isSpectator()) {
            uoOpenersCounter.decrementOpeners(livingEntity, getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return uoOpenersCounter.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
    }

    @Override
    public void recheckOpen() {
        if (!remove) {
            uoOpenersCounter.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
        }
    }

    private static void playChestSound(Level level, BlockPos pos, BlockState state, SoundEvent soundEvent) {
        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.LEFT) {
            return;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        if (type == ChestType.RIGHT) {
            net.minecraft.core.Direction direction = ChestBlock.getConnectedDirection(state);
            x += direction.getStepX() * 0.5D;
            z += direction.getStepZ() * 0.5D;
        }

        level.playSound(
                null,
                x,
                y,
                z,
                soundEvent,
                SoundSource.BLOCKS,
                0.5F,
                level.random.nextFloat() * 0.1F + 0.9F
        );
    }

}
