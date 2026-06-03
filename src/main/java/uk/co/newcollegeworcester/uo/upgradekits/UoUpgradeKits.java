package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.function.Function;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class UoUpgradeKits implements ModInitializer {
    public static final String MOD_ID = "uo_upgrade_kits";

    private static final EnumMap<Tier, Item> UPGRADE_KITS = new EnumMap<>(Tier.class);
    private static final EnumMap<Tier, Item> CONVERSION_KITS = new EnumMap<>(Tier.class);
    private static final Map<Identifier, EnumMap<Tier, Identifier>> UPGRADE_TARGETS_BY_SOURCE = new HashMap<>();
    private static final Map<Identifier, EnumMap<Tier, Identifier>> CONVERSION_TARGETS_BY_SOURCE = new HashMap<>();
    private static final ThreadLocal<Set<BlockPos>> SUPPRESSED_DROP_POSITIONS = ThreadLocal.withInitial(HashSet::new);
    private static Block copperFurnace;
    private static Block copperSmoker;
    private static Block copperBlastFurnace;
    public static BlockEntityType<CopperFurnaceBlockEntity> COPPER_FURNACE_BLOCK_ENTITY;
    public static BlockEntityType<CopperSmokerBlockEntity> COPPER_SMOKER_BLOCK_ENTITY;
    public static BlockEntityType<CopperBlastFurnaceBlockEntity> COPPER_BLAST_FURNACE_BLOCK_ENTITY;

    @Override
    public void onInitialize() {
        registerBlocks();
        registerBlockEntityTypes();

        for (Tier tier : Tier.values()) {
            UPGRADE_KITS.put(tier, registerItem(tier.id + "_upgrade_kit"));
            CONVERSION_KITS.put(tier, registerItem(tier.id + "_conversion_kit"));
        }

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(copperFurnace);
            entries.accept(copperSmoker);
            entries.accept(copperBlastFurnace);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            for (Tier tier : Tier.values()) {
                entries.accept(UPGRADE_KITS.get(tier));
                entries.accept(CONVERSION_KITS.get(tier));
            }
        });

        registerUpgradeTargets();
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            Tier tier = kitTier(stack.getItem());
            if (tier == null || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }

            return applyKit(player, world, hitResult.getBlockPos(), stack, tier, isConversionKit(stack.getItem()));
        });
    }

    private static void registerBlocks() {
        copperFurnace = registerBlock("copper_furnace", CopperFurnaceBlock::new, Blocks.FURNACE);
        copperSmoker = registerBlock("copper_smoker", CopperSmokerBlock::new, Blocks.SMOKER);
        copperBlastFurnace = registerBlock("copper_blast_furnace", CopperBlastFurnaceBlock::new, Blocks.BLAST_FURNACE);
    }

    private static void registerBlockEntityTypes() {
        COPPER_FURNACE_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("copper_furnace"),
                FabricBlockEntityTypeBuilder.create(CopperFurnaceBlockEntity::new, copperFurnace).build()
        );
        COPPER_SMOKER_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("copper_smoker"),
                FabricBlockEntityTypeBuilder.create(CopperSmokerBlockEntity::new, copperSmoker).build()
        );
        COPPER_BLAST_FURNACE_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("copper_blast_furnace"),
                FabricBlockEntityTypeBuilder.create(CopperBlastFurnaceBlockEntity::new, copperBlastFurnace).build()
        );
    }

    private static Block registerBlock(String path, Function<BlockBehaviour.Properties, Block> factory, Block copyFrom) {
        Identifier id = id(path);
        ResourceKey<Block> blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        Block block = factory.apply(BlockBehaviour.Properties.ofFullCopy(copyFrom).setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
        return block;
    }

    private static Item registerItem(String path) {
        Identifier id = id(path);
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).stacksTo(64)));
    }

    private static InteractionResult applyKit(Player player, Level world, BlockPos pos, ItemStack stack, Tier kitTier, boolean conversion) {
        Identifier sourceId = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock());
        if (!conversion && isVanillaToIronUpgrade(sourceId, kitTier)) {
            conversion = true;
        }

        EnumMap<Tier, Identifier> targets = conversion
                ? CONVERSION_TARGETS_BY_SOURCE.getOrDefault(sourceId, UPGRADE_TARGETS_BY_SOURCE.get(sourceId))
                : UPGRADE_TARGETS_BY_SOURCE.get(sourceId);
        if (targets == null) {
            return InteractionResult.PASS;
        }

        Tier currentTier = currentTier(sourceId, targets);
        Tier targetTier = conversion ? kitTier : upgradeTargetTier(currentTier, kitTier);
        if (targetTier == null || (currentTier != null && targetTier.ordinal() <= currentTier.ordinal())) {
            return InteractionResult.PASS;
        }

        Identifier targetId = targets.get(targetTier);
        if (targetId == null || targetId.equals(sourceId)) {
            return InteractionResult.PASS;
        }

        Optional<Block> target = BuiltInRegistries.BLOCK.getOptional(targetId);
        if (target.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity oldBlockEntity = world.getBlockEntity(pos);
        boolean useSavedInventoryData = isVanillaSupportedSource(sourceId);
        CapturedInventory capturedInventory = useSavedInventoryData ? null : captureInventory(oldBlockEntity);
        CompoundTag oldBlockEntityData = oldBlockEntity == null
                ? null
                : oldBlockEntity.saveWithFullMetadata(world.registryAccess());
        if (!useSavedInventoryData && capturedInventory != null && oldBlockEntityData != null) {
            oldBlockEntityData.remove("Items");
        }
        prepareForUpgrade(oldBlockEntity, useSavedInventoryData);

        BlockState oldState = world.getBlockState(pos);
        BlockState newState = copySharedProperties(oldState, target.get().defaultBlockState());
        if (!useSavedInventoryData && oldBlockEntity != null) {
            world.removeBlockEntity(pos);
        }
        SUPPRESSED_DROP_POSITIONS.get().add(pos.immutable());
        try {
            if (useSavedInventoryData) {
                world.setBlockAndUpdate(pos, newState);
            } else {
                world.setBlock(pos, newState, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            }
        } finally {
            SUPPRESSED_DROP_POSITIONS.get().remove(pos);
        }
        restoreBlockEntityData(world, pos, oldBlockEntityData);
        if (!useSavedInventoryData) {
            restoreInventory(world.getBlockEntity(pos), capturedInventory);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    public static boolean isSuppressingContainerDrops(BlockPos pos) {
        return SUPPRESSED_DROP_POSITIONS.get().contains(pos);
    }

    private static CapturedInventory captureInventory(BlockEntity blockEntity) {
        if (!(blockEntity instanceof Container container)) {
            return null;
        }

        List<ItemStack> items = new ArrayList<>(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            items.add(container.getItem(slot).copy());
        }
        return new CapturedInventory(items);
    }

    private static void prepareForUpgrade(BlockEntity blockEntity, boolean useSavedInventoryData) {
        if (blockEntity == null) {
            return;
        }

        invokeIfPresent(blockEntity, "setUpgrading", boolean.class, true);

        if (blockEntity instanceof Container container) {
            if (useSavedInventoryData) {
                container.clearContent();
            } else {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    container.setItem(slot, ItemStack.EMPTY);
                }
            }
            container.setChanged();
        }
    }

    private static void restoreBlockEntityData(Level world, BlockPos pos, CompoundTag blockEntityData) {
        if (blockEntityData == null) {
            return;
        }

        BlockEntity newBlockEntity = world.getBlockEntity(pos);
        if (newBlockEntity == null) {
            return;
        }

        blockEntityData.putInt("x", pos.getX());
        blockEntityData.putInt("y", pos.getY());
        blockEntityData.putInt("z", pos.getZ());
        newBlockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), blockEntityData));
        newBlockEntity.setChanged();
    }

    private static void restoreInventory(BlockEntity blockEntity, CapturedInventory capturedInventory) {
        if (capturedInventory == null || !(blockEntity instanceof Container container)) {
            return;
        }

        int size = Math.min(container.getContainerSize(), capturedInventory.items().size());
        for (int slot = 0; slot < size; slot++) {
            container.setItem(slot, capturedInventory.items().get(slot).copy());
        }
        container.setChanged();
    }

    private static void invokeIfPresent(Object target, String methodName) {
        try {
            target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void invokeIfPresent(Object target, String methodName, Class<?> parameterType, Object value) {
        try {
            target.getClass().getMethod(methodName, parameterType).invoke(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static BlockState copySharedProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> sourceProperty : from.getProperties()) {
            Property<?> targetProperty = result.getProperties().stream()
                    .filter(property -> property.getName().equals(sourceProperty.getName()))
                    .findFirst()
                    .orElse(null);
            if (targetProperty != null) {
                result = copyProperty(from, result, sourceProperty, targetProperty);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState from,
            BlockState to,
            Property<T> sourceProperty,
            Property<?> targetProperty
    ) {
        T value = from.getValue(sourceProperty);
        @SuppressWarnings("unchecked")
        Property<T> typedTarget = (Property<T>) targetProperty;
        if (typedTarget.getPossibleValues().contains(value)) {
            return to.setValue(typedTarget, value);
        }
        return to;
    }

    private static Tier kitTier(Item item) {
        for (Tier tier : Tier.values()) {
            if (UPGRADE_KITS.get(tier) == item || CONVERSION_KITS.get(tier) == item) {
                return tier;
            }
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        if ("better-furnaces-and-chests".equals(itemId.getNamespace())) {
            for (Tier tier : Tier.values()) {
                if (itemId.getPath().equals(tier.id + "_upgrade_kit")) {
                    return tier;
                }
            }
        }
        return null;
    }

    private static boolean isConversionKit(Item item) {
        return CONVERSION_KITS.containsValue(item);
    }

    private static boolean isVanillaToIronUpgrade(Identifier sourceId, Tier kitTier) {
        return kitTier == Tier.IRON
                && isVanillaSupportedSource(sourceId);
    }

    private static boolean isVanillaSupportedSource(Identifier sourceId) {
        return "minecraft".equals(sourceId.getNamespace())
                && UPGRADE_TARGETS_BY_SOURCE.containsKey(sourceId);
    }

    private static Tier currentTier(Identifier sourceId, EnumMap<Tier, Identifier> targets) {
        for (Map.Entry<Tier, Identifier> entry : targets.entrySet()) {
            if (entry.getValue().equals(sourceId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Tier upgradeTargetTier(Tier currentTier, Tier kitTier) {
        if (currentTier == null) {
            return kitTier == Tier.COPPER ? Tier.COPPER : null;
        }

        return kitTier.ordinal() == currentTier.ordinal() + 1 ? kitTier : null;
    }

    private static void registerUpgradeTargets() {
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:chest", "reinfchest:%s_chest");
        registerChestConversionTargets();

        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:barrel", "reinfbarrel:%s_barrel");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:shulker_box", "reinfshulker:%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:white_shulker_box", "reinfshulker:white_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:orange_shulker_box", "reinfshulker:orange_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:magenta_shulker_box", "reinfshulker:magenta_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:light_blue_shulker_box", "reinfshulker:light_blue_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:yellow_shulker_box", "reinfshulker:yellow_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:lime_shulker_box", "reinfshulker:lime_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:pink_shulker_box", "reinfshulker:pink_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:gray_shulker_box", "reinfshulker:gray_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:light_gray_shulker_box", "reinfshulker:light_gray_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:cyan_shulker_box", "reinfshulker:cyan_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:purple_shulker_box", "reinfshulker:purple_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:blue_shulker_box", "reinfshulker:blue_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:brown_shulker_box", "reinfshulker:brown_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:green_shulker_box", "reinfshulker:green_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:red_shulker_box", "reinfshulker:red_%s_shulker_box");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:black_shulker_box", "reinfshulker:black_%s_shulker_box");

        registerPartialFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:furnace", "uo_upgrade_kits:%s_furnace", Tier.COPPER);
        registerPartialFamily(UPGRADE_TARGETS_BY_SOURCE, "uo_upgrade_kits:copper_furnace", "better-furnaces-and-chests:%s_furnace", Tier.IRON, Tier.GOLD, Tier.DIAMOND, Tier.NETHERITE);
        registerFurnaceConversionTargets();
        registerPartialFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:smoker", "uo_upgrade_kits:%s_smoker", Tier.COPPER);
        registerPartialFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:blast_furnace", "uo_upgrade_kits:%s_blast_furnace", Tier.COPPER);
    }

    private static void registerChestConversionTargets() {
        EnumMap<Tier, Identifier> targets = new EnumMap<>(Tier.class);
        targets.put(Tier.COPPER, Identifier.parse("reinfchest:copper_chest"));
        targets.put(Tier.IRON, Identifier.parse("better-furnaces-and-chests:iron_chest"));
        targets.put(Tier.GOLD, Identifier.parse("better-furnaces-and-chests:gold_chest"));
        targets.put(Tier.DIAMOND, Identifier.parse("better-furnaces-and-chests:diamond_chest"));
        targets.put(Tier.NETHERITE, Identifier.parse("better-furnaces-and-chests:netherite_chest"));

        CONVERSION_TARGETS_BY_SOURCE.put(Identifier.parse("minecraft:chest"), targets);
        registerPatternSources(CONVERSION_TARGETS_BY_SOURCE, targets, "reinfchest:%s_chest", Tier.values());
        registerPatternSources(CONVERSION_TARGETS_BY_SOURCE, targets, "better-furnaces-and-chests:%s_chest", Tier.IRON, Tier.GOLD, Tier.DIAMOND, Tier.NETHERITE);
    }

    private static void registerFurnaceConversionTargets() {
        EnumMap<Tier, Identifier> targets = new EnumMap<>(Tier.class);
        targets.put(Tier.COPPER, Identifier.parse("uo_upgrade_kits:copper_furnace"));
        targets.put(Tier.IRON, Identifier.parse("better-furnaces-and-chests:iron_furnace"));
        targets.put(Tier.GOLD, Identifier.parse("better-furnaces-and-chests:gold_furnace"));
        targets.put(Tier.DIAMOND, Identifier.parse("better-furnaces-and-chests:diamond_furnace"));
        targets.put(Tier.NETHERITE, Identifier.parse("better-furnaces-and-chests:netherite_furnace"));

        CONVERSION_TARGETS_BY_SOURCE.put(Identifier.parse("minecraft:furnace"), targets);
        CONVERSION_TARGETS_BY_SOURCE.put(Identifier.parse("uo_upgrade_kits:copper_furnace"), targets);
        registerPatternSources(CONVERSION_TARGETS_BY_SOURCE, targets, "better-furnaces-and-chests:%s_furnace", Tier.IRON, Tier.GOLD, Tier.DIAMOND, Tier.NETHERITE);
    }

    private static void registerFamily(Map<Identifier, EnumMap<Tier, Identifier>> map, String vanillaSource, String targetPattern) {
        registerPartialFamily(map, vanillaSource, targetPattern, Tier.values());
    }

    private static void registerPartialFamily(Map<Identifier, EnumMap<Tier, Identifier>> map, String vanillaSource, String targetPattern, Tier... supportedTiers) {
        EnumMap<Tier, Identifier> targets = new EnumMap<>(Tier.class);
        for (Tier tier : supportedTiers) {
            targets.put(tier, Identifier.parse(String.format(Locale.ROOT, targetPattern, tier.id)));
        }

        map.put(Identifier.parse(vanillaSource), targets);
        registerPatternSources(map, targets, targetPattern, supportedTiers);
    }

    private static void registerPatternSources(Map<Identifier, EnumMap<Tier, Identifier>> map, EnumMap<Tier, Identifier> targets, String sourcePattern, Tier... sourceTiers) {
        for (Tier tier : sourceTiers) {
            map.put(Identifier.parse(String.format(Locale.ROOT, sourcePattern, tier.id)), targets);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private record CapturedInventory(List<ItemStack> items) {
    }

    private enum Tier {
        COPPER("copper"),
        IRON("iron"),
        GOLD("gold"),
        DIAMOND("diamond"),
        NETHERITE("netherite");

        final String id;

        Tier(String id) {
            this.id = id;
        }

    }
}
