package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.ChatFormatting;

public enum UpgradeTier {
    COPPER("copper", 45, 2, 2, 8, 2.0D, ChatFormatting.GOLD),
    IRON("iron", 63, 4, 4, 6, 4.0D, ChatFormatting.GRAY),
    GOLD("gold", 81, 8, 8, 4, 10.0D, ChatFormatting.YELLOW),
    DIAMOND("diamond", 99, 12, 16, 2, 20.0D, ChatFormatting.AQUA),
    NETHERITE("netherite", 117, 16, 32, 1, 100.0D, ChatFormatting.DARK_GRAY);

    public final String id;
    public final int storageSlots;
    public final int bundleCapacity;
    public final int hopperBatchSize;
    public final int hopperCooldownTicks;
    public final double cookingSpeedMultiplier;
    public final ChatFormatting tooltipColor;
    private final int[] storageSlotIndexes;

    UpgradeTier(
            String id,
            int storageSlots,
            int bundleCapacity,
            int hopperBatchSize,
            int hopperCooldownTicks,
            double cookingSpeedMultiplier,
            ChatFormatting tooltipColor
    ) {
        this.id = id;
        this.storageSlots = storageSlots;
        this.bundleCapacity = bundleCapacity;
        this.hopperBatchSize = hopperBatchSize;
        this.hopperCooldownTicks = hopperCooldownTicks;
        this.cookingSpeedMultiplier = cookingSpeedMultiplier;
        this.tooltipColor = tooltipColor;
        this.storageSlotIndexes = new int[storageSlots];
        for (int slot = 0; slot < storageSlots; slot++) {
            storageSlotIndexes[slot] = slot;
        }
    }

    public static UpgradeTier fromPath(String path) {
        for (UpgradeTier tier : values()) {
            if (path.startsWith(tier.id + "_") || path.contains("_" + tier.id + "_")) {
                return tier;
            }
        }
        return null;
    }

    int[] storageSlotIndexes() {
        return storageSlotIndexes;
    }
}
