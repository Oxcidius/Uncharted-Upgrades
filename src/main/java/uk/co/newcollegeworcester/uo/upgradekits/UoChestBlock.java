package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public final class UoChestBlock extends ChestBlock {
    private final Supplier<BlockEntityType<? extends ChestBlockEntity>> blockEntityType;
    private final UpgradeTier tier;

    public UoChestBlock(
            Supplier<BlockEntityType<? extends ChestBlockEntity>> blockEntityType,
            UpgradeTier tier,
            BlockBehaviour.Properties properties
    ) {
        super(blockEntityType, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
        this.blockEntityType = blockEntityType;
        this.tier = tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UoChestBlockEntity(blockEntityType.get(), pos, state, tier);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level instanceof ServerLevel serverLevel) {
            Container container = ChestBlock.getContainer(this, state, level, pos, false);
            if (container != null) {
                int slots = container.getContainerSize();
                Component title = getTitle(level, pos);
                player.openMenu(createMenuProvider(container, slots, title));
                player.awardStat(getOpenChestStat());
                PiglinAi.angerNearbyPiglins(serverLevel, player, true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private Component getTitle(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof UoChestBlockEntity chest && chest.getCustomName() != null) {
            return chest.getCustomName();
        }
        return Component.translatable("block." + UoUpgradeKits.MOD_ID + "." + tier.id + "_chest");
    }

    private static ExtendedMenuProvider<Integer> createMenuProvider(
            Container container,
            int slots,
            Component title
    ) {
        return new ExtendedMenuProvider<>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer player) {
                return slots;
            }

            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new UoChestMenu(containerId, inventory, container, slots);
            }
        };
    }
}
