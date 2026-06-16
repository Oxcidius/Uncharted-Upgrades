package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BundleContents;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

final class UoItems {
    private static final EnumMap<UpgradeTier, Item> UPGRADE_KITS = new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, Item> CONVERSION_KITS = new EnumMap<>(UpgradeTier.class);
    private static final EnumMap<UpgradeTier, List<Item>> BUNDLES = new EnumMap<>(UpgradeTier.class);
    private static Item kitTemplate;

    private UoItems() {
    }

    static void register() {
        kitTemplate = registerItem("kit_template");
        for (UpgradeTier tier : UpgradeTier.values()) {
            UPGRADE_KITS.put(tier, registerItem(tier.id + "_upgrade_kit"));
            CONVERSION_KITS.put(tier, registerItem(tier.id + "_conversion_kit"));

            List<Item> tierBundles = new ArrayList<>();
            tierBundles.add(registerBundleItem(tier, null));
            for (UoShulkerVariant variant : UoShulkerVariant.values()) {
                if (variant != UoShulkerVariant.UNDYED) {
                    tierBundles.add(registerBundleItem(tier, variant));
                }
            }
            BUNDLES.put(tier, List.copyOf(tierBundles));
        }
    }

    static Item kitTemplate() {
        return kitTemplate;
    }

    static Item upgradeKit(UpgradeTier tier) {
        return UPGRADE_KITS.get(tier);
    }

    static Item conversionKit(UpgradeTier tier) {
        return CONVERSION_KITS.get(tier);
    }

    static boolean isConversionKit(Item item) {
        return CONVERSION_KITS.containsValue(item);
    }

    static List<Item> bundles(UpgradeTier tier) {
        return BUNDLES.get(tier);
    }

    private static Item registerItem(String path) {
        Identifier id = UoRegistries.id(path);
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        return Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new Item(new Item.Properties().setId(key).stacksTo(64))
        );
    }

    private static Item registerBundleItem(UpgradeTier tier, UoShulkerVariant variant) {
        String path = variant == null
                ? tier.id + "_bundle"
                : tier.id + "_" + variant.id + "_bundle";
        Identifier id = UoRegistries.id(path);
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return Registry.register(BuiltInRegistries.ITEM, key, new TieredBundleItem(tier, properties));
    }
}
