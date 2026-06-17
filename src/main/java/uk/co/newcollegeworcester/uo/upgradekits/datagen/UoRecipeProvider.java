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

final class UoRecipeProvider implements DataProvider {
    private static final String MOD_ID = "uncharted_upgrades";
    private static final String[] TIERS = {"copper", "iron", "gold", "diamond", "netherite"};
    private static final String[] INGREDIENTS = {
            "minecraft:copper_ingot",
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:diamond",
            "minecraft:netherite_ingot"
    };
    private static final String[] COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
    private static final String[] STANDARD_FAMILIES = {
            "chest", "barrel", "hopper", "furnace", "smoker", "blast_furnace"
    };

    private final Path recipesPath;
    private final Path recipeAdvancementsPath;

    UoRecipeProvider(FabricPackOutput output) {
        Path modDataPath = output.getOutputFolder()
                .resolve("data")
                .resolve(MOD_ID);
        this.recipesPath = modDataPath.resolve("recipe");
        this.recipeAdvancementsPath = modDataPath
                .resolve("advancement")
                .resolve("recipes")
                .resolve("misc");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        generateKitRecipes(output, writes);
        generateSmithingRecipes(output, writes);
        generateDyeRecipes(output, writes);
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Uncharted Upgrades recipes";
    }

    private void generateKitRecipes(CachedOutput output, List<CompletableFuture<?>> writes) {
        JsonObject template = shaped(
                "misc",
                List.of("ccc", "clc", "ccc"),
                key("c", "#uncharted_upgrades:cobblestone_variants", "l", "minecraft:leather"),
                mod("kit_template"),
                3
        );
        save(output, writes, "kit_template", template);
        saveRecipeUnlock(
                output,
                writes,
                "kit_template",
                "has_leather",
                "minecraft:leather",
                "has_cobblestone",
                "#uncharted_upgrades:cobblestone_variants"
        );

        for (int tierIndex = 0; tierIndex < TIERS.length; tierIndex++) {
            String tier = TIERS[tierIndex];
            save(output, writes, tier + "_upgrade_kit", shaped(
                    "misc",
                    List.of("aaa", "aba", "aaa"),
                    key("a", INGREDIENTS[tierIndex], "b", mod("kit_template")),
                    mod(tier + "_upgrade_kit"),
                    1
            ));
            saveRecipeUnlock(
                    output,
                    writes,
                    tier + "_upgrade_kit",
                    "has_kit_template",
                    mod("kit_template"),
                    "has_tier_ingredient",
                    INGREDIENTS[tierIndex]
            );

            JsonArray ingredients = new JsonArray();
            for (int includedTier = 0; includedTier <= tierIndex; includedTier++) {
                ingredients.add(mod(TIERS[includedTier] + "_upgrade_kit"));
            }
            JsonObject conversion = new JsonObject();
            conversion.addProperty("type", "minecraft:crafting_shapeless");
            conversion.addProperty("category", "misc");
            conversion.add("ingredients", ingredients);
            conversion.add("result", result(mod(tier + "_conversion_kit"), 1));
            save(output, writes, tier + "_conversion_kit", conversion);
            saveRecipeUnlock(
                    output,
                    writes,
                    tier + "_conversion_kit",
                    "has_upgrade_kit",
                    mod(tier + "_upgrade_kit")
            );
        }
    }

    private void generateSmithingRecipes(CachedOutput output, List<CompletableFuture<?>> writes) {
        for (AssetVariant variant : variants()) {
            for (int targetIndex = 0; targetIndex < TIERS.length; targetIndex++) {
                String targetTier = TIERS[targetIndex];
                String upgradeBase = targetIndex == 0
                        ? variant.vanillaId()
                        : variant.tieredId(TIERS[targetIndex - 1]);
                save(
                        output,
                        writes,
                        variant.upgradeRecipeName(targetTier),
                        smithing(
                                upgradeBase,
                                mod(targetTier + "_upgrade_kit"),
                                variant.tieredId(targetTier)
                        )
                );

                save(
                        output,
                        writes,
                        variant.conversionRecipeName(null, targetTier),
                        smithing(
                                variant.vanillaId(),
                                mod(targetTier + "_conversion_kit"),
                                variant.tieredId(targetTier)
                        )
                );
                for (int sourceIndex = 0; sourceIndex < targetIndex; sourceIndex++) {
                    String sourceTier = TIERS[sourceIndex];
                    save(
                            output,
                            writes,
                            variant.conversionRecipeName(sourceTier, targetTier),
                            smithing(
                                    variant.tieredId(sourceTier),
                                    mod(targetTier + "_conversion_kit"),
                                    variant.tieredId(targetTier)
                            )
                    );
                }
            }
        }
    }

    private void generateDyeRecipes(CachedOutput output, List<CompletableFuture<?>> writes) {
        for (String tier : TIERS) {
            for (String color : COLORS) {
                JsonObject bundle = transmute(
                        "equipment",
                        "bundle_dye",
                        "#uncharted_upgrades:" + tier + "_bundles",
                        "minecraft:" + color + "_dye",
                        mod(tier + "_" + color + "_bundle")
                );
                save(output, writes, "dye_" + tier + "_bundle_" + color, bundle);

                JsonObject shulker = transmute(
                        "misc",
                        "uo_" + tier + "_shulker_box_dye",
                        "#uncharted_upgrades:" + tier + "_shulker_boxes",
                        "minecraft:" + color + "_dye",
                        mod(color + "_" + tier + "_shulker_box")
                );
                save(output, writes, "dye_" + tier + "_shulker_box_" + color, shulker);
            }
        }
    }

    private static List<AssetVariant> variants() {
        List<AssetVariant> variants = new ArrayList<>();
        for (String family : STANDARD_FAMILIES) {
            variants.add(new AssetVariant(family, null));
        }
        variants.add(new AssetVariant("bundle", null));
        variants.add(new AssetVariant("shulker_box", null));
        for (String color : COLORS) {
            variants.add(new AssetVariant("bundle", color));
            variants.add(new AssetVariant("shulker_box", color));
        }
        return variants;
    }

    private static JsonObject shaped(
            String category,
            List<String> pattern,
            JsonObject key,
            String resultId,
            int count
    ) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", category);
        JsonArray patternJson = new JsonArray();
        pattern.forEach(patternJson::add);
        recipe.add("pattern", patternJson);
        recipe.add("key", key);
        recipe.add("result", result(resultId, count));
        return recipe;
    }

    private static JsonObject key(String... entries) {
        JsonObject key = new JsonObject();
        for (int index = 0; index < entries.length; index += 2) {
            key.addProperty(entries[index], entries[index + 1]);
        }
        return key;
    }

    private static JsonObject smithing(String base, String addition, String resultId) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:smithing_transform");
        recipe.addProperty("template", mod("kit_template"));
        recipe.addProperty("base", base);
        recipe.addProperty("addition", addition);
        recipe.add("result", result(resultId, null));
        return recipe;
    }

    private static JsonObject transmute(
            String category,
            String group,
            String input,
            String material,
            String resultId
    ) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_transmute");
        recipe.addProperty("category", category);
        recipe.addProperty("group", group);
        recipe.addProperty("input", input);
        recipe.addProperty("material", material);
        recipe.add("result", result(resultId, null));
        return recipe;
    }

    private static JsonObject result(String id, Integer count) {
        JsonObject result = new JsonObject();
        result.addProperty("id", id);
        if (count != null) {
            result.addProperty("count", count);
        }
        return result;
    }

    private void save(
            CachedOutput output,
            List<CompletableFuture<?>> writes,
            String name,
            JsonObject json
    ) {
        writes.add(DataProvider.saveStable(output, json, recipesPath.resolve(name + ".json")));
    }

    private void saveRecipeUnlock(
            CachedOutput output,
            List<CompletableFuture<?>> writes,
            String recipeName,
            String... inventoryCriteria
    ) {
        JsonObject advancement = new JsonObject();
        advancement.addProperty("parent", "minecraft:recipes/root");

        JsonObject criteria = new JsonObject();
        JsonArray requirements = new JsonArray();
        JsonArray requirementSet = new JsonArray();
        for (int index = 0; index < inventoryCriteria.length; index += 2) {
            String criterionName = inventoryCriteria[index];
            criteria.add(criterionName, inventoryCriterion(inventoryCriteria[index + 1]));
            requirementSet.add(criterionName);
        }
        criteria.add("has_the_recipe", recipeUnlockedCriterion(mod(recipeName)));
        requirementSet.add("has_the_recipe");
        requirements.add(requirementSet);

        JsonObject rewards = new JsonObject();
        JsonArray recipes = new JsonArray();
        recipes.add(mod(recipeName));
        rewards.add("recipes", recipes);

        advancement.add("criteria", criteria);
        advancement.add("requirements", requirements);
        advancement.add("rewards", rewards);
        writes.add(DataProvider.saveStable(output, advancement, recipeAdvancementsPath.resolve(recipeName + ".json")));
    }

    private static JsonObject inventoryCriterion(String itemOrTag) {
        JsonObject criterion = new JsonObject();
        criterion.addProperty("trigger", "minecraft:inventory_changed");

        JsonObject item = new JsonObject();
        item.addProperty("items", itemOrTag);
        JsonArray items = new JsonArray();
        items.add(item);

        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        criterion.add("conditions", conditions);
        return criterion;
    }

    private static JsonObject recipeUnlockedCriterion(String recipeId) {
        JsonObject criterion = new JsonObject();
        criterion.addProperty("trigger", "minecraft:recipe_unlocked");
        JsonObject conditions = new JsonObject();
        conditions.addProperty("recipe", recipeId);
        criterion.add("conditions", conditions);
        return criterion;
    }

    private static String mod(String path) {
        return MOD_ID + ":" + path;
    }

    private record AssetVariant(String family, String color) {
        String vanillaId() {
            if ("bundle".equals(family)) {
                return "minecraft:" + (color == null ? "bundle" : color + "_bundle");
            }
            if ("shulker_box".equals(family)) {
                return "minecraft:" + (color == null ? "shulker_box" : color + "_shulker_box");
            }
            return "minecraft:" + family;
        }

        String tieredId(String tier) {
            if ("bundle".equals(family)) {
                return mod(tier + "_" + (color == null ? "" : color + "_") + "bundle");
            }
            if ("shulker_box".equals(family)) {
                return mod((color == null ? "" : color + "_") + tier + "_shulker_box");
            }
            return mod(tier + "_" + family);
        }

        String upgradeRecipeName(String targetTier) {
            if ("bundle".equals(family) && color != null) {
                return "smithing_upgrade_bundle_to_" + targetTier + "_" + color;
            }
            return "smithing_upgrade_" + variantName() + "_to_" + targetTier;
        }

        String conversionRecipeName(String sourceTier, String targetTier) {
            String prefix = "smithing_conversion_" + variantName();
            if (sourceTier != null) {
                prefix += "_" + sourceTier;
            }
            String suffix = "_to_" + targetTier;
            if ("bundle".equals(family) && color != null) {
                suffix += "_" + color;
            }
            return prefix + suffix;
        }

        private String variantName() {
            if ("bundle".equals(family)) {
                return "bundle";
            }
            if ("shulker_box".equals(family) && color != null) {
                return color + "_shulker_box";
            }
            return family;
        }
    }
}
