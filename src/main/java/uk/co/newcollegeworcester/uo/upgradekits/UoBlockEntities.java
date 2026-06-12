package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;
import java.util.EnumMap;

final class UoBlockEntities {
    private static final EnumMap<UpgradeTier, BlockEntityType<UoChestBlockEntity>> CHESTS =
            new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, BlockEntityType<UoBarrelBlockEntity>> BARRELS =
            new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, BlockEntityType<UoHopperBlockEntity>> HOPPERS =
            new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, BlockEntityType<UoShulkerBlockEntity>> SHULKERS =
            new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, EnumMap<CookingType, BlockEntityType<TieredCookingBlockEntity>>> COOKING =
            new EnumMap<>(UpgradeTier.class);

    private UoBlockEntities() {
    }

    static void register() {
        for (UpgradeTier tier : UpgradeTier.values()) {
            registerCooking(tier);
            CHESTS.put(tier, Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    UoRegistries.id(tier.id + "_chest"),
                    FabricBlockEntityTypeBuilder.create(
                            (pos, state) -> new UoChestBlockEntity(chest(tier), pos, state, tier),
                            UoBlocks.chest(tier)
                    ).build()
            ));
            BARRELS.put(tier, Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    UoRegistries.id(tier.id + "_barrel"),
                    FabricBlockEntityTypeBuilder.create(
                            (pos, state) -> new UoBarrelBlockEntity(barrel(tier), pos, state, tier),
                            UoBlocks.barrel(tier)
                    ).build()
            ));
            HOPPERS.put(tier, Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    UoRegistries.id(tier.id + "_hopper"),
                    FabricBlockEntityTypeBuilder.create(
                            (pos, state) -> new UoHopperBlockEntity(hopper(tier), pos, state, tier),
                            UoBlocks.hopper(tier)
                    ).build()
            ));

            Block[] shulkerBlocks = UoBlocks.shulkers(tier).values().toArray(Block[]::new);
            SHULKERS.put(tier, Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    UoRegistries.id(tier.id + "_shulker_box"),
                    FabricBlockEntityTypeBuilder.create(
                            (pos, state) -> new UoShulkerBlockEntity(
                                    shulker(tier),
                                    pos,
                                    state,
                                    tier,
                                    ((UoShulkerBlock) state.getBlock()).getVariant()
                            ),
                            shulkerBlocks
                    ).build()
            ));
        }
    }

    static BlockEntityType<UoChestBlockEntity> chest(UpgradeTier tier) {
        return CHESTS.get(tier);
    }

    static BlockEntityType<UoBarrelBlockEntity> barrel(UpgradeTier tier) {
        return BARRELS.get(tier);
    }

    static BlockEntityType<UoHopperBlockEntity> hopper(UpgradeTier tier) {
        return HOPPERS.get(tier);
    }

    static BlockEntityType<UoShulkerBlockEntity> shulker(UpgradeTier tier) {
        return SHULKERS.get(tier);
    }

    static BlockEntityType<TieredCookingBlockEntity> cooking(UpgradeTier tier, CookingType type) {
        return COOKING.get(tier).get(type);
    }

    static Collection<BlockEntityType<UoChestBlockEntity>> chestTypes() {
        return CHESTS.values();
    }

    static Collection<BlockEntityType<UoShulkerBlockEntity>> shulkerTypes() {
        return SHULKERS.values();
    }

    private static void registerCooking(UpgradeTier tier) {
        EnumMap<CookingType, BlockEntityType<TieredCookingBlockEntity>> tierTypes =
                new EnumMap<>(CookingType.class);
        COOKING.put(tier, tierTypes);
        for (CookingType type : CookingType.values()) {
            tierTypes.put(type, Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    UoRegistries.id(tier.id + "_" + type.id),
                    FabricBlockEntityTypeBuilder.create(
                            (pos, state) -> new TieredCookingBlockEntity(
                                    cooking(tier, type),
                                    pos,
                                    state,
                                    tier,
                                    type
                            ),
                            UoBlocks.cooking(tier, type)
                    ).build()
            ));
        }
    }
}
