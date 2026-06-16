package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class UnchartedUpgradesTags {
    public static final TagKey<Block> CHESTS = block("chests");
    public static final TagKey<Block> BARRELS = block("barrels");
    public static final TagKey<Block> SHULKER_BOXES = block("shulker_boxes");
    public static final TagKey<Block> HOPPERS = block("hoppers");
    public static final TagKey<Block> COOKING_BLOCKS = block("cooking_blocks");
    public static final TagKey<Block> STORAGE_BLOCKS = block("storage_blocks");
    public static final TagKey<Block> FUNCTIONAL_BLOCKS = block("functional_blocks");

    public static final TagKey<Item> CHEST_ITEMS = item("chests");
    public static final TagKey<Item> BARREL_ITEMS = item("barrels");
    public static final TagKey<Item> SHULKER_BOX_ITEMS = item("shulker_boxes");
    public static final TagKey<Item> HOPPER_ITEMS = item("hoppers");
    public static final TagKey<Item> COOKING_BLOCK_ITEMS = item("cooking_blocks");
    public static final TagKey<Item> STORAGE_BLOCK_ITEMS = item("storage_blocks");
    public static final TagKey<Item> FUNCTIONAL_BLOCK_ITEMS = item("functional_blocks");
    public static final TagKey<Item> FUNCTIONAL_ITEMS = item("functional_items");
    public static final TagKey<Item> BUNDLES = item("bundles");
    public static final TagKey<Item> KITS = item("kits");
    public static final TagKey<Item> UPGRADE_KITS = item("upgrade_kits");
    public static final TagKey<Item> CONVERSION_KITS = item("conversion_kits");

    private UnchartedUpgradesTags() {
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(UoUpgradeKits.MOD_ID, path)
        );
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(UoUpgradeKits.MOD_ID, path)
        );
    }
}
