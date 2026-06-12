package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class UoAdvancementService {
    private static final String MAX_UPGRADE_ADVANCEMENT = "progression/max_upgrade";
    private static final String MAX_UPGRADE_CRITERION = "used_netherite_conversion";

    private UoAdvancementService() {
    }

    static void awardPlacedBlockUpgrade(Player player, boolean conversion, UpgradeTier tier) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        awardFunctionalUpgrade(serverPlayer, conversion, true);
        if (conversion && tier == UpgradeTier.NETHERITE) {
            award(serverPlayer, MAX_UPGRADE_ADVANCEMENT, MAX_UPGRADE_CRITERION);
        }
    }

    public static void awardSmithingUpgrade(Player player, ItemStack addition, ItemStack result) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        UpgradeTier conversionTier = kitTier(addition.getItem(), true);
        boolean conversion = conversionTier != null;
        if (!conversion && kitTier(addition.getItem(), false) == null) {
            return;
        }

        awardFunctionalUpgrade(serverPlayer, conversion, result.getItem() instanceof BlockItem);
        if (conversionTier == UpgradeTier.NETHERITE) {
            award(serverPlayer, MAX_UPGRADE_ADVANCEMENT, MAX_UPGRADE_CRITERION);
        }
    }

    private static void awardFunctionalUpgrade(
            ServerPlayer player,
            boolean conversion,
            boolean block
    ) {
        String operation = conversion ? "converted" : "upgrade";
        String target = block ? "functional_block" : "functional_item";
        String criterion = conversion
                ? "used_conversion_kit_on_" + (block ? "block" : "item")
                : "used_upgrade_kit_on_" + (block ? "block" : "item");
        award(player, "progression/" + operation + "_" + target, criterion);
    }

    private static UpgradeTier kitTier(Item item, boolean conversion) {
        for (UpgradeTier tier : UpgradeTier.values()) {
            Item kit = conversion
                    ? UoRegistries.conversionKit(tier)
                    : UoRegistries.upgradeKit(tier);
            if (kit == item) {
                return tier;
            }
        }
        return null;
    }

    private static void award(ServerPlayer player, String advancementPath, String criterion) {
        AdvancementHolder advancement = player.level()
                .getServer()
                .getAdvancements()
                .get(UoRegistries.id(advancementPath));
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
