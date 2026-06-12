package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;

public final class UoUpgradeKits implements ModInitializer {
    public static final String MOD_ID = "uncharted_upgrades";

    @Override
    public void onInitialize() {
        UoRegistries.register();
        UoNetworking.register();
        UoUpgradeService.register();
        UoLootIntegration.register();
    }

    public static boolean isSuppressingContainerDrops(BlockPos pos) {
        return UoUpgradeService.isSuppressingContainerDrops(pos);
    }

    public static Collection<BlockEntityType<UoShulkerBlockEntity>> shulkerBlockEntityTypes() {
        return UoRegistries.shulkerBlockEntityTypes();
    }
}
