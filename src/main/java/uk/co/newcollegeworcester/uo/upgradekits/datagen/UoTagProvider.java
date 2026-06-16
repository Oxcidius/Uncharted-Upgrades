package uk.co.newcollegeworcester.uo.upgradekits.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class UoTagProvider implements DataProvider {
    private static final String MOD_ID = "uncharted_upgrades";
    private static final String[] TIERS = {"copper", "iron", "gold", "diamond", "netherite"};
    private static final String[] COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
    private static final String[] COOKING_TYPES = {"furnace", "smoker", "blast_furnace"};

    private final Path dataPath;

    UoTagProvider(FabricPackOutput output) {
        this.dataPath = output.getOutputFolder().resolve("data");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        generateMinecraftTags(output, writes);
        generateBlockTags(output, writes);
        generateItemTags(output, writes);
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Uncharted Upgrades tags";
    }

    private void generateMinecraftTags(CachedOutput output, List<CompletableFuture<?>> writes) {
        save(output, writes, "minecraft", "block/features_cannot_replace",
                tag(List.of("#" + MOD_ID + ":chests"), true));
        save(output, writes, "minecraft", "block/guarded_by_piglins",
                tag(List.of(
                        "#" + MOD_ID + ":chests",
                        "#" + MOD_ID + ":barrels",
                        "#" + MOD_ID + ":shulker_boxes"
                ), true));
        save(output, writes, "minecraft", "block/mineable/axe",
                tag(List.of("#" + MOD_ID + ":chests", "#" + MOD_ID + ":barrels"), true));
        save(output, writes, "minecraft", "block/mineable/pickaxe",
                tag(List.of(
                        "#" + MOD_ID + ":cooking_blocks",
                        "#" + MOD_ID + ":shulker_boxes",
                        "#" + MOD_ID + ":hoppers"
                ), true));
        save(output, writes, "minecraft", "block/shulker_boxes",
                tag(List.of("#" + MOD_ID + ":shulker_boxes"), true));

        List<String> bundleTags = new ArrayList<>();
        for (String tier : TIERS) {
            bundleTags.add("#" + MOD_ID + ":" + tier + "_bundles");
        }
        save(output, writes, "minecraft", "item/bundles", tag(bundleTags, true));
    }

    private void generateBlockTags(CachedOutput output, List<CompletableFuture<?>> writes) {
        save(output, writes, MOD_ID, "block/chests", tag(tieredIds("chest"), true));
        save(output, writes, MOD_ID, "block/barrels", tag(tieredIds("barrel"), true));
        save(output, writes, MOD_ID, "block/hoppers", tag(tieredIds("hopper"), true));
        save(output, writes, MOD_ID, "block/storage_blocks", tag(List.of(
                "#" + MOD_ID + ":chests",
                "#" + MOD_ID + ":barrels",
                "#" + MOD_ID + ":shulker_boxes"
        ), true));

        List<String> cookingBlocks = new ArrayList<>();
        for (String tier : TIERS) {
            for (String cookingType : COOKING_TYPES) {
                cookingBlocks.add(mod(tier + "_" + cookingType));
            }
        }
        save(output, writes, MOD_ID, "block/cooking_blocks", tag(cookingBlocks, true));

        List<String> shulkers = new ArrayList<>();
        for (String tier : TIERS) {
            shulkers.add(mod(tier + "_shulker_box"));
            for (String color : COLORS) {
                shulkers.add(mod(color + "_" + tier + "_shulker_box"));
            }
        }
        save(output, writes, MOD_ID, "block/shulker_boxes", tag(shulkers, true));
        save(output, writes, MOD_ID, "block/functional_blocks", tag(List.of(
                "#" + MOD_ID + ":storage_blocks",
                "#" + MOD_ID + ":hoppers",
                "#" + MOD_ID + ":cooking_blocks"
        ), true));
    }

    private void generateItemTags(CachedOutput output, List<CompletableFuture<?>> writes) {
        save(output, writes, MOD_ID, "item/cobblestone_variants", tag(List.of(
                "minecraft:cobblestone",
                "minecraft:mossy_cobblestone",
                "minecraft:cobbled_deepslate",
                "minecraft:blackstone"
        ), false));

        save(output, writes, MOD_ID, "item/chests", tag(tieredIds("chest"), true));
        save(output, writes, MOD_ID, "item/barrels", tag(tieredIds("barrel"), true));
        save(output, writes, MOD_ID, "item/hoppers", tag(tieredIds("hopper"), true));
        save(output, writes, MOD_ID, "item/upgrade_kits", tag(tieredIds("upgrade_kit"), true));
        save(output, writes, MOD_ID, "item/conversion_kits", tag(tieredIds("conversion_kit"), true));

        List<String> cookingItems = new ArrayList<>();
        for (String tier : TIERS) {
            for (String cookingType : COOKING_TYPES) {
                cookingItems.add(mod(tier + "_" + cookingType));
            }
        }
        save(output, writes, MOD_ID, "item/cooking_blocks", tag(cookingItems, true));

        List<String> allBundles = new ArrayList<>();
        List<String> allShulkers = new ArrayList<>();
        for (String tier : TIERS) {
            List<String> bundles = new ArrayList<>();
            bundles.add(mod(tier + "_bundle"));
            for (String color : COLORS) {
                bundles.add(mod(tier + "_" + color + "_bundle"));
            }
            save(output, writes, MOD_ID, "item/" + tier + "_bundles", tag(bundles, true));
            allBundles.add("#" + MOD_ID + ":" + tier + "_bundles");

            List<String> shulkers = new ArrayList<>();
            shulkers.add(mod(tier + "_shulker_box"));
            for (String color : COLORS) {
                shulkers.add(mod(color + "_" + tier + "_shulker_box"));
            }
            save(output, writes, MOD_ID, "item/" + tier + "_shulker_boxes", tag(shulkers, true));
            allShulkers.add("#" + MOD_ID + ":" + tier + "_shulker_boxes");
        }
        save(output, writes, MOD_ID, "item/bundles", tag(allBundles, true));
        save(output, writes, MOD_ID, "item/shulker_boxes", tag(allShulkers, true));
        save(output, writes, MOD_ID, "item/storage_blocks", tag(List.of(
                "#" + MOD_ID + ":chests",
                "#" + MOD_ID + ":barrels",
                "#" + MOD_ID + ":shulker_boxes"
        ), true));
        save(output, writes, MOD_ID, "item/functional_items", tag(List.of(
                "#" + MOD_ID + ":bundles",
                "#" + MOD_ID + ":shulker_boxes",
                "#" + MOD_ID + ":hoppers"
        ), true));
        save(output, writes, MOD_ID, "item/functional_blocks", tag(List.of(
                "#" + MOD_ID + ":storage_blocks",
                "#" + MOD_ID + ":hoppers",
                "#" + MOD_ID + ":cooking_blocks"
        ), true));
        save(output, writes, MOD_ID, "item/kits", tag(List.of(
                MOD_ID + ":kit_template",
                "#" + MOD_ID + ":upgrade_kits",
                "#" + MOD_ID + ":conversion_kits"
        ), true));
    }

    private List<String> tieredIds(String suffix) {
        List<String> ids = new ArrayList<>();
        for (String tier : TIERS) {
            ids.add(mod(tier + "_" + suffix));
        }
        return ids;
    }

    private JsonObject tag(List<String> values, boolean includeReplace) {
        JsonObject tag = new JsonObject();
        if (includeReplace) {
            tag.addProperty("replace", false);
        }
        JsonArray entries = new JsonArray();
        values.forEach(entries::add);
        tag.add("values", entries);
        return tag;
    }

    private void save(
            CachedOutput output,
            List<CompletableFuture<?>> writes,
            String namespace,
            String path,
            JsonObject json
    ) {
        writes.add(DataProvider.saveStable(
                output,
                json,
                dataPath.resolve(namespace).resolve("tags").resolve(path + ".json")
        ));
    }

    private String mod(String path) {
        return MOD_ID + ":" + path;
    }
}
