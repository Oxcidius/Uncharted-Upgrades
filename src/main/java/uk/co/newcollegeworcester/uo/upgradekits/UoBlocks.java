package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.function.Function;

final class UoBlocks {
    private static final EnumMap<UpgradeTier, Block> CHESTS = new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, Block> BARRELS = new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, Block> HOPPERS = new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, EnumMap<CookingType, Block>> COOKING_BLOCKS =
            new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, EnumMap<UoShulkerVariant, Block>> SHULKER_BLOCKS =
            new EnumMap<>(UpgradeTier.class);

    private UoBlocks() {
    }

    static void register() {
        for (UpgradeTier tier : UpgradeTier.values()) {
            registerCookingBlocks(tier);
            CHESTS.put(tier, registerBlock(
                    tier.id + "_chest",
                    properties -> new UoChestBlock(() -> UoBlockEntities.chest(tier), tier, properties),
                    Blocks.CHEST
            ));
            BARRELS.put(tier, registerBlock(
                    tier.id + "_barrel",
                    properties -> new UoBarrelBlock(() -> UoBlockEntities.barrel(tier), tier, properties),
                    Blocks.BARREL
            ));
            HOPPERS.put(tier, registerBlock(
                    tier.id + "_hopper",
                    properties -> new UoHopperBlock(() -> UoBlockEntities.hopper(tier), tier, properties),
                    Blocks.HOPPER
            ));
            registerShulkerBlocks(tier);
        }
    }

    static Block chest(UpgradeTier tier) {
        return CHESTS.get(tier);
    }

    static Block barrel(UpgradeTier tier) {
        return BARRELS.get(tier);
    }

    static Block hopper(UpgradeTier tier) {
        return HOPPERS.get(tier);
    }

    static Block cooking(UpgradeTier tier, CookingType type) {
        return COOKING_BLOCKS.get(tier).get(type);
    }

    static EnumMap<UoShulkerVariant, Block> shulkers(UpgradeTier tier) {
        return SHULKER_BLOCKS.get(tier);
    }

    private static void registerCookingBlocks(UpgradeTier tier) {
        EnumMap<CookingType, Block> tierBlocks = new EnumMap<>(CookingType.class);
        COOKING_BLOCKS.put(tier, tierBlocks);
        for (CookingType type : CookingType.values()) {
            tierBlocks.put(type, registerBlock(
                    tier.id + "_" + type.id,
                    properties -> createCookingBlock(tier, type, properties),
                    type.vanillaBlock()
            ));
        }
    }

    private static void registerShulkerBlocks(UpgradeTier tier) {
        EnumMap<UoShulkerVariant, Block> tierBlocks = new EnumMap<>(UoShulkerVariant.class);
        SHULKER_BLOCKS.put(tier, tierBlocks);
        for (UoShulkerVariant variant : UoShulkerVariant.values()) {
            Block vanillaBlock = ShulkerBoxBlock.getBlockByColor(variant.color);
            tierBlocks.put(variant, registerBlock(
                    variant.blockId(tier),
                    properties -> new UoShulkerBlock(
                            () -> UoBlockEntities.shulker(tier),
                            tier,
                            variant,
                            properties
                    ),
                    vanillaBlock
            ));
        }
    }

    private static Block createCookingBlock(
            UpgradeTier tier,
            CookingType type,
            BlockBehaviour.Properties properties
    ) {
        return switch (type) {
            case FURNACE -> new TieredFurnaceBlock(
                    () -> UoBlockEntities.cooking(tier, type),
                    tier,
                    properties
            );
            case SMOKER -> new TieredSmokerBlock(
                    () -> UoBlockEntities.cooking(tier, type),
                    tier,
                    properties
            );
            case BLAST_FURNACE -> new TieredBlastFurnaceBlock(
                    () -> UoBlockEntities.cooking(tier, type),
                    tier,
                    properties
            );
        };
    }

    private static Block registerBlock(
            String path,
            Function<BlockBehaviour.Properties, Block> factory,
            Block copyFrom
    ) {
        Identifier id = UoRegistries.id(path);
        ResourceKey<Block> blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(copyFrom).setId(blockKey);
        if (copyFrom == Blocks.FURNACE
                || copyFrom == Blocks.SMOKER
                || copyFrom == Blocks.BLAST_FURNACE) {
            properties = properties.lightLevel(state -> state.getValue(FurnaceBlock.LIT) ? 13 : 0);
        }
        Block block = factory.apply(properties);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item.Properties itemProperties = new Item.Properties().setId(itemKey);
        if (block instanceof UoShulkerBlock) {
            itemProperties = itemProperties.stacksTo(1);
        }
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, itemProperties));
        return block;
    }
}
