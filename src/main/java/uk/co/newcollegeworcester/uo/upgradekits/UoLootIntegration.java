package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

final class UoLootIntegration {
    private static final Set<Identifier> STRUCTURE_CHESTS = Set.of(
            chest("abandoned_mineshaft"),
            chest("ancient_city"),
            chest("bastion_bridge"),
            chest("bastion_hoglin_stable"),
            chest("bastion_other"),
            chest("bastion_treasure"),
            chest("buried_treasure"),
            chest("desert_pyramid"),
            chest("end_city_treasure"),
            chest("jungle_temple"),
            chest("shipwreck_supply"),
            chest("shipwreck_treasure"),
            chest("simple_dungeon"),
            chest("stronghold_corridor"),
            chest("stronghold_crossing"),
            chest("stronghold_library"),
            chest("woodland_mansion")
    );

    private UoLootIntegration() {
    }

    static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin() || !STRUCTURE_CHESTS.contains(key.identifier())) {
                return;
            }

            addChancePool(tableBuilder, UoItems.kitTemplate(), 0.18F);
            addTierPools(tableBuilder, UpgradeTier.COPPER, 0.10F, 0.07F);
            addTierPools(tableBuilder, UpgradeTier.IRON, 0.07F, 0.045F);
            addTierPools(tableBuilder, UpgradeTier.GOLD, 0.045F, 0.025F);
            addTierPools(tableBuilder, UpgradeTier.DIAMOND, 0.025F, 0.0125F);
            addTierPools(tableBuilder, UpgradeTier.NETHERITE, 0.008F, 0.004F);
        });
    }

    private static void addTierPools(
            net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder,
            UpgradeTier tier,
            float upgradeChance,
            float conversionChance
    ) {
        addChancePool(tableBuilder, UoItems.upgradeKit(tier), upgradeChance);
        addChancePool(tableBuilder, UoItems.conversionKit(tier), conversionChance);
    }

    private static void addChancePool(
            net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder,
            Item item,
            float chance
    ) {
        tableBuilder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(item))
                .when(LootItemRandomChanceCondition.randomChance(chance)));
    }

    private static Identifier chest(String path) {
        return Identifier.withDefaultNamespace("chests/" + path);
    }
}
