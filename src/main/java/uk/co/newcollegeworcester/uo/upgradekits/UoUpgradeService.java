package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class UoUpgradeService {
    private static final Map<Identifier, EnumMap<UpgradeTier, Identifier>> UPGRADE_TARGETS_BY_SOURCE =
            new HashMap<>();
    private static final Map<Identifier, EnumMap<UpgradeTier, Identifier>> CONVERSION_TARGETS_BY_SOURCE =
            new HashMap<>();
    private static final ThreadLocal<Set<BlockPos>> SUPPRESSED_DROP_POSITIONS = new ThreadLocal<>();

    private UoUpgradeService() {
    }

    static void register() {
        registerUpgradeTargets();
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            UpgradeTier tier = kitTier(stack.getItem());
            if (tier == null || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            return applyKit(
                    player,
                    world,
                    hitResult.getBlockPos(),
                    stack,
                    tier,
                    isConversionKit(stack.getItem())
            );
        });
    }

    static boolean isSuppressingContainerDrops(BlockPos pos) {
        Set<BlockPos> positions = SUPPRESSED_DROP_POSITIONS.get();
        return positions != null && positions.contains(pos);
    }

    private static InteractionResult applyKit(
            Player player,
            Level world,
            BlockPos pos,
            ItemStack stack,
            UpgradeTier kitTier,
            boolean conversion
    ) {
        Identifier sourceId = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock());
        EnumMap<UpgradeTier, Identifier> targets = conversion
                ? CONVERSION_TARGETS_BY_SOURCE.getOrDefault(sourceId, UPGRADE_TARGETS_BY_SOURCE.get(sourceId))
                : UPGRADE_TARGETS_BY_SOURCE.get(sourceId);
        if (targets == null) {
            return InteractionResult.PASS;
        }

        UpgradeTier currentTier = currentTier(sourceId, targets);
        UpgradeTier targetTier = conversion ? kitTier : upgradeTargetTier(currentTier, kitTier);
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

        BlockState newState = copySharedProperties(
                world.getBlockState(pos),
                target.get().defaultBlockState()
        );
        if (!useSavedInventoryData && oldBlockEntity != null) {
            world.removeBlockEntity(pos);
        }
        Set<BlockPos> suppressedPositions = SUPPRESSED_DROP_POSITIONS.get();
        if (suppressedPositions == null) {
            suppressedPositions = new HashSet<>();
            SUPPRESSED_DROP_POSITIONS.set(suppressedPositions);
        }
        suppressedPositions.add(pos.immutable());
        try {
            if (useSavedInventoryData) {
                world.setBlockAndUpdate(pos, newState);
            } else {
                world.setBlock(
                        pos,
                        newState,
                        Block.UPDATE_ALL
                                | Block.UPDATE_SUPPRESS_DROPS
                                | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
                );
            }
        } finally {
            suppressedPositions.remove(pos);
            if (suppressedPositions.isEmpty()) {
                SUPPRESSED_DROP_POSITIONS.remove();
            }
        }

        restoreBlockEntityData(world, pos, oldBlockEntityData);
        if (!useSavedInventoryData) {
            restoreInventory(world.getBlockEntity(pos), capturedInventory);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        UoAdvancementService.awardPlacedBlockUpgrade(player, conversion, targetTier);
        return InteractionResult.SUCCESS_SERVER;
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
        newBlockEntity.loadWithComponents(
                TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), blockEntityData)
        );
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

    private static void invokeIfPresent(
            Object target,
            String methodName,
            Class<?> parameterType,
            Object value
    ) {
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
        return typedTarget.getPossibleValues().contains(value)
                ? to.setValue(typedTarget, value)
                : to;
    }

    private static UpgradeTier kitTier(Item item) {
        for (UpgradeTier tier : UpgradeTier.values()) {
            if (UoRegistries.upgradeKit(tier) == item || UoRegistries.conversionKit(tier) == item) {
                return tier;
            }
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        if ("better-furnaces-and-chests".equals(itemId.getNamespace())) {
            for (UpgradeTier tier : UpgradeTier.values()) {
                if (itemId.getPath().equals(tier.id + "_upgrade_kit")) {
                    return tier;
                }
            }
        }
        return null;
    }

    private static boolean isConversionKit(Item item) {
        return UoRegistries.isConversionKit(item);
    }

    private static boolean isVanillaSupportedSource(Identifier sourceId) {
        return "minecraft".equals(sourceId.getNamespace())
                && UPGRADE_TARGETS_BY_SOURCE.containsKey(sourceId);
    }

    private static UpgradeTier currentTier(
            Identifier sourceId,
            EnumMap<UpgradeTier, Identifier> targets
    ) {
        for (Map.Entry<UpgradeTier, Identifier> entry : targets.entrySet()) {
            if (entry.getValue().equals(sourceId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static UpgradeTier upgradeTargetTier(UpgradeTier currentTier, UpgradeTier kitTier) {
        if (currentTier == null) {
            return kitTier == UpgradeTier.COPPER ? UpgradeTier.COPPER : null;
        }
        return kitTier.ordinal() == currentTier.ordinal() + 1 ? kitTier : null;
    }

    private static void registerUpgradeTargets() {
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:chest", "uncharted_upgrades:%s_chest");
        registerConversionFamily("minecraft:chest", "uncharted_upgrades:%s_chest");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:barrel", "uncharted_upgrades:%s_barrel");
        registerConversionFamily("minecraft:barrel", "uncharted_upgrades:%s_barrel");
        for (UoShulkerVariant variant : UoShulkerVariant.values()) {
            String targetPattern = variant.id.isEmpty()
                    ? "uncharted_upgrades:%s_shulker_box"
                    : "uncharted_upgrades:" + variant.id + "_%s_shulker_box";
            registerFamily(UPGRADE_TARGETS_BY_SOURCE, variant.vanillaBlockId(), targetPattern);
            registerConversionFamily(variant.vanillaBlockId(), targetPattern);
        }
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:furnace", "uncharted_upgrades:%s_furnace");
        registerConversionFamily("minecraft:furnace", "uncharted_upgrades:%s_furnace");
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:smoker", "uncharted_upgrades:%s_smoker");
        registerConversionFamily("minecraft:smoker", "uncharted_upgrades:%s_smoker");
        registerFamily(
                UPGRADE_TARGETS_BY_SOURCE,
                "minecraft:blast_furnace",
                "uncharted_upgrades:%s_blast_furnace"
        );
        registerConversionFamily(
                "minecraft:blast_furnace",
                "uncharted_upgrades:%s_blast_furnace"
        );
        registerFamily(UPGRADE_TARGETS_BY_SOURCE, "minecraft:hopper", "uncharted_upgrades:%s_hopper");
        registerConversionFamily("minecraft:hopper", "uncharted_upgrades:%s_hopper");
    }

    private static void registerConversionFamily(String vanillaSource, String targetPattern) {
        EnumMap<UpgradeTier, Identifier> targets = new EnumMap<>(UpgradeTier.class);
        for (UpgradeTier tier : UpgradeTier.values()) {
            targets.put(tier, Identifier.parse(String.format(Locale.ROOT, targetPattern, tier.id)));
        }
        CONVERSION_TARGETS_BY_SOURCE.put(Identifier.parse(vanillaSource), targets);
        registerPatternSources(CONVERSION_TARGETS_BY_SOURCE, targets, targetPattern, UpgradeTier.values());
    }

    private static void registerFamily(
            Map<Identifier, EnumMap<UpgradeTier, Identifier>> map,
            String vanillaSource,
            String targetPattern
    ) {
        EnumMap<UpgradeTier, Identifier> targets = new EnumMap<>(UpgradeTier.class);
        for (UpgradeTier tier : UpgradeTier.values()) {
            targets.put(tier, Identifier.parse(String.format(Locale.ROOT, targetPattern, tier.id)));
        }
        map.put(Identifier.parse(vanillaSource), targets);
        registerPatternSources(map, targets, targetPattern, UpgradeTier.values());
    }

    private static void registerPatternSources(
            Map<Identifier, EnumMap<UpgradeTier, Identifier>> map,
            EnumMap<UpgradeTier, Identifier> targets,
            String sourcePattern,
            UpgradeTier... sourceTiers
    ) {
        for (UpgradeTier tier : sourceTiers) {
            map.put(
                    Identifier.parse(String.format(Locale.ROOT, sourcePattern, tier.id)),
                    targets
            );
        }
    }

    private record CapturedInventory(List<ItemStack> items) {
    }
}
