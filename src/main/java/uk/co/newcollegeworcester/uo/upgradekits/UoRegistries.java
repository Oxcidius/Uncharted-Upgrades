package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;

final class UoRegistries {
    private UoRegistries() {
    }

    static void register() {
        UoBlocks.register();
        UoBlockEntities.register();
        UoMenus.register();
        UoItems.register();
        UoCreativeTab.register();
    }

    static MenuType<UoChestMenu> chestMenu() {
        return UoMenus.chest();
    }

    static Collection<BlockEntityType<UoChestBlockEntity>> chestBlockEntityTypes() {
        return UoBlockEntities.chestTypes();
    }

    static Collection<BlockEntityType<UoShulkerBlockEntity>> shulkerBlockEntityTypes() {
        return UoBlockEntities.shulkerTypes();
    }

    static Item upgradeKit(UpgradeTier tier) {
        return UoItems.upgradeKit(tier);
    }

    static Item conversionKit(UpgradeTier tier) {
        return UoItems.conversionKit(tier);
    }

    static boolean isConversionKit(Item item) {
        return UoItems.isConversionKit(item);
    }

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(UoUpgradeKits.MOD_ID, path);
    }
}
