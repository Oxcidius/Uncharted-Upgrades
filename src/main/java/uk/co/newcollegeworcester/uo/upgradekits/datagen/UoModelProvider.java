package uk.co.newcollegeworcester.uo.upgradekits.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class UoModelProvider implements DataProvider {
    private static final String MOD_ID = "uncharted_upgrades";
    private static final String[] TIERS = {"copper", "iron", "gold", "diamond", "netherite"};
    private static final String[] COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
    private static final String[] COOKING_TYPES = {"furnace", "smoker", "blast_furnace"};

    private final Path assetsPath;

    UoModelProvider(FabricDataOutput output) {
        this.assetsPath = output.getOutputFolder().resolve("assets").resolve(MOD_ID);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (int tierIndex = 0; tierIndex < TIERS.length; tierIndex++) {
            String tier = TIERS[tierIndex];
            generateBlockStates(output, writes, tier);
            generateBlockModels(output, writes, tier);
            generateItemModels(output, writes, tier, tierIndex);
            generateItemDefinitions(output, writes, tier);
        }
        generateKitModelsAndDefinitions(output, writes);
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Uncharted Upgrades models, blockstates, and item definitions";
    }

    private void generateBlockStates(CachedOutput output, List<CompletableFuture<?>> writes, String tier) {
        save(output, writes, "blockstates/" + tier + "_barrel", barrelBlockState(tier));
        save(output, writes, "blockstates/" + tier + "_chest", chestBlockState(tier));
        save(output, writes, "blockstates/" + tier + "_hopper", hopperBlockState(tier));
        for (String cookingType : COOKING_TYPES) {
            save(output, writes, "blockstates/" + tier + "_" + cookingType,
                    cookingBlockState(tier, cookingType));
        }
        save(output, writes, "blockstates/" + tier + "_shulker_box", shulkerBlockState());
        for (String color : COLORS) {
            save(output, writes, "blockstates/" + color + "_" + tier + "_shulker_box",
                    shulkerBlockState());
        }
    }

    private void generateBlockModels(CachedOutput output, List<CompletableFuture<?>> writes, String tier) {
        save(output, writes, "models/block/" + tier + "_barrel", barrelModel(tier, false));
        save(output, writes, "models/block/" + tier + "_barrel_open", barrelModel(tier, true));
        save(output, writes, "models/block/" + tier + "_chest", chestParticleModel(tier));
        save(output, writes, "models/block/" + tier + "_hopper", hopperModel(tier, false));
        save(output, writes, "models/block/" + tier + "_hopper_side", hopperModel(tier, true));
        for (String cookingType : COOKING_TYPES) {
            save(output, writes, "models/block/" + tier + "_" + cookingType,
                    cookingModel(tier, cookingType, false));
            save(output, writes, "models/block/" + tier + "_" + cookingType + "_on",
                    cookingModel(tier, cookingType, true));
        }
    }

    private void generateItemModels(
            CachedOutput output,
            List<CompletableFuture<?>> writes,
            String tier,
            int tierIndex
    ) {
        save(output, writes, "models/item/" + tier + "_hopper",
                generatedItemModel(mod("item/" + tier + "_hopper")));

        // These legacy aliases are retained because they are present in the existing resource set.
        if (tierIndex > 0) {
            for (String cookingType : COOKING_TYPES) {
                save(output, writes, "models/item/" + tier + "_" + cookingType,
                        parentModel(mod("block/" + tier + "_" + cookingType)));
            }
        }

        generateBundleModels(output, writes, tier, null);
        for (String color : COLORS) {
            generateBundleModels(output, writes, tier, color);
        }
    }

    private void generateBundleModels(
            CachedOutput output,
            List<CompletableFuture<?>> writes,
            String tier,
            String color
    ) {
        String id = bundleId(tier, color);
        String vanillaPrefix = color == null ? "bundle" : color + "_bundle";
        save(output, writes, "models/item/" + id, texturedParentModel(
                "minecraft:item/" + vanillaPrefix,
                mod("item/" + id)
        ));
        save(output, writes, "models/item/" + id + "_open_back",
                parentModel("minecraft:item/" + vanillaPrefix + "_open_back"));
        save(output, writes, "models/item/" + id + "_open_front",
                parentModel("minecraft:item/" + vanillaPrefix + "_open_front"));
    }

    private void generateItemDefinitions(
            CachedOutput output,
            List<CompletableFuture<?>> writes,
            String tier
    ) {
        save(output, writes, "items/" + tier + "_barrel",
                simpleItemDefinition(mod("block/" + tier + "_barrel")));
        save(output, writes, "items/" + tier + "_hopper",
                simpleItemDefinition(mod("item/" + tier + "_hopper")));
        for (String cookingType : COOKING_TYPES) {
            save(output, writes, "items/" + tier + "_" + cookingType,
                    simpleItemDefinition(mod("block/" + tier + "_" + cookingType)));
        }
        save(output, writes, "items/" + tier + "_chest", chestItemDefinition(tier));
        save(output, writes, "items/" + tier + "_shulker_box", shulkerItemDefinition(tier, null));
        for (String color : COLORS) {
            save(output, writes, "items/" + color + "_" + tier + "_shulker_box",
                    shulkerItemDefinition(tier, color));
        }

        save(output, writes, "items/" + tier + "_bundle", bundleItemDefinition(tier, null));
        for (String color : COLORS) {
            save(output, writes, "items/" + tier + "_" + color + "_bundle",
                    bundleItemDefinition(tier, color));
        }
    }

    private void generateKitModelsAndDefinitions(
            CachedOutput output,
            List<CompletableFuture<?>> writes
    ) {
        save(output, writes, "models/item/kit_template",
                generatedItemModel(mod("item/kit_template")));
        save(output, writes, "items/kit_template",
                simpleItemDefinition(mod("item/kit_template")));
        for (String tier : TIERS) {
            for (String kitType : List.of("upgrade_kit", "conversion_kit")) {
                String id = tier + "_" + kitType;
                save(output, writes, "models/item/" + id, generatedItemModel(mod("item/" + id)));
                save(output, writes, "items/" + id, simpleItemDefinition(mod("item/" + id)));
            }
        }
    }

    private JsonObject barrelBlockState(String tier) {
        JsonObject variants = new JsonObject();
        String closed = mod("block/" + tier + "_barrel");
        String open = mod("block/" + tier + "_barrel_open");
        addVariant(variants, "facing=up,open=false", closed, null, null);
        addVariant(variants, "facing=down,open=false", closed, 180, null);
        addVariant(variants, "facing=north,open=false", closed, 90, null);
        addVariant(variants, "facing=east,open=false", closed, 90, 90);
        addVariant(variants, "facing=south,open=false", closed, 90, 180);
        addVariant(variants, "facing=west,open=false", closed, 90, 270);
        addVariant(variants, "facing=up,open=true", open, null, null);
        addVariant(variants, "facing=down,open=true", open, 180, null);
        addVariant(variants, "facing=north,open=true", open, 90, null);
        addVariant(variants, "facing=east,open=true", open, 90, 90);
        addVariant(variants, "facing=south,open=true", open, 90, 180);
        addVariant(variants, "facing=west,open=true", open, 90, 270);
        return wrapped("variants", variants);
    }

    private JsonObject cookingBlockState(String tier, String cookingType) {
        JsonObject variants = new JsonObject();
        String off = mod("block/" + tier + "_" + cookingType);
        String on = off + "_on";
        for (boolean lit : List.of(false, true)) {
            String model = lit ? on : off;
            addVariant(variants, "facing=north,lit=" + lit, model, null, null);
            addVariant(variants, "facing=east,lit=" + lit, model, null, 90);
            addVariant(variants, "facing=south,lit=" + lit, model, null, 180);
            addVariant(variants, "facing=west,lit=" + lit, model, null, 270);
        }
        return wrapped("variants", variants);
    }

    private JsonObject chestBlockState(String tier) {
        JsonArray multipart = new JsonArray();
        String model = mod("block/" + tier + "_chest");
        addMultipart(multipart, "north", model, null);
        addMultipart(multipart, "east", model, 90);
        addMultipart(multipart, "south", model, 180);
        addMultipart(multipart, "west", model, 270);
        JsonObject root = new JsonObject();
        root.add("multipart", multipart);
        return root;
    }

    private JsonObject hopperBlockState(String tier) {
        JsonObject variants = new JsonObject();
        String down = mod("block/" + tier + "_hopper");
        String side = mod("block/" + tier + "_hopper_side");
        addVariant(variants, "facing=down", down, null, null);
        addVariant(variants, "facing=east", side, null, 90);
        addVariant(variants, "facing=north", side, null, null);
        addVariant(variants, "facing=south", side, null, 180);
        addVariant(variants, "facing=west", side, null, 270);
        return wrapped("variants", variants);
    }

    private JsonObject shulkerBlockState() {
        JsonObject emptyVariant = new JsonObject();
        emptyVariant.addProperty("model", "minecraft:block/shulker_box");
        JsonObject variants = new JsonObject();
        variants.add("", emptyVariant);
        return wrapped("variants", variants);
    }

    private JsonObject barrelModel(String tier, boolean open) {
        JsonObject textures = new JsonObject();
        textures.addProperty("bottom", mod("block/" + tier + "_barrel_bottom"));
        textures.addProperty("side", mod("block/" + tier + "_barrel_side"));
        textures.addProperty("top", mod("block/" + tier + "_barrel_top" + (open ? "_open" : "")));
        return model("minecraft:block/cube_bottom_top", textures);
    }

    private JsonObject cookingModel(String tier, String cookingType, boolean lit) {
        String suffix = lit ? "_on" : "";
        JsonObject textures = new JsonObject();
        textures.addProperty("front", mod("block/" + tier + "_" + cookingType + "_front" + suffix));
        textures.addProperty("side", mod("block/" + tier + "_" + cookingType + "_side"));
        textures.addProperty("top", mod("block/" + tier + "_" + cookingType + "_top"));
        if (cookingType.equals("smoker")) {
            textures.addProperty("bottom", mod("block/" + tier + "_smoker_bottom"));
        }
        String parent = cookingType.equals("furnace")
                ? "minecraft:block/orientable"
                : "minecraft:block/" + cookingType + suffix;
        return model(parent, textures);
    }

    private JsonObject chestParticleModel(String tier) {
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:block/" + tier + "_block");
        JsonObject root = new JsonObject();
        root.add("textures", textures);
        return root;
    }

    private JsonObject hopperModel(String tier, boolean sideModel) {
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", mod("block/" + tier + "_hopper_outside"));
        textures.addProperty("top", mod("block/" + tier + "_hopper_top"));
        textures.addProperty("side", mod("block/" + tier + "_hopper_outside"));
        textures.addProperty("inside", mod("block/" + tier + "_hopper_inside"));
        return model("minecraft:block/" + (sideModel ? "hopper_side" : "hopper"), textures);
    }

    private JsonObject chestItemDefinition(String tier) {
        JsonObject special = new JsonObject();
        special.addProperty("type", "minecraft:chest");
        special.addProperty("texture", mod(tier + "/single"));
        JsonObject rootModel = new JsonObject();
        rootModel.addProperty("type", "minecraft:special");
        rootModel.addProperty("base", "minecraft:item/chest");
        rootModel.add("model", special);
        return wrapped("model", rootModel);
    }

    private JsonObject shulkerItemDefinition(String tier, String color) {
        JsonObject special = new JsonObject();
        special.addProperty("type", "minecraft:shulker_box");
        special.addProperty("texture", mod(tier + "/shulker" + (color == null ? "" : "_" + color)));
        JsonObject rootModel = new JsonObject();
        rootModel.addProperty("type", "minecraft:special");
        rootModel.addProperty("base", "minecraft:item/shulker_box");
        rootModel.add("model", special);
        return wrapped("model", rootModel);
    }

    private JsonObject bundleItemDefinition(String tier, String color) {
        String id = bundleId(tier, color);
        JsonObject closed = modelReference(mod("item/" + id));
        JsonArray openModels = new JsonArray();
        openModels.add(modelReference(mod("item/" + id + "_open_back")));
        JsonObject selectedItem = new JsonObject();
        selectedItem.addProperty("type", "minecraft:bundle/selected_item");
        openModels.add(selectedItem);
        openModels.add(modelReference(mod("item/" + id + "_open_front")));

        JsonObject composite = new JsonObject();
        composite.addProperty("type", "minecraft:composite");
        composite.add("models", openModels);

        JsonObject condition = new JsonObject();
        condition.addProperty("type", "minecraft:condition");
        condition.add("on_false", closed);
        condition.add("on_true", composite);
        condition.addProperty("property", "minecraft:bundle/has_selected_item");

        JsonObject guiCase = new JsonObject();
        guiCase.add("model", condition);
        guiCase.addProperty("when", "gui");
        JsonArray cases = new JsonArray();
        cases.add(guiCase);

        JsonObject selection = new JsonObject();
        selection.addProperty("type", "minecraft:select");
        selection.add("cases", cases);
        selection.add("fallback", closed);
        selection.addProperty("property", "minecraft:display_context");
        return wrapped("model", selection);
    }

    private JsonObject simpleItemDefinition(String model) {
        return wrapped("model", modelReference(model));
    }

    private JsonObject modelReference(String resource) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", resource);
        return model;
    }

    private JsonObject generatedItemModel(String texture) {
        return texturedParentModel("minecraft:item/generated", texture);
    }

    private JsonObject texturedParentModel(String parent, String texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texture);
        return model(parent, textures);
    }

    private JsonObject parentModel(String parent) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", parent);
        return root;
    }

    private JsonObject model(String parent, JsonObject textures) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", parent);
        root.add("textures", textures);
        return root;
    }

    private JsonObject wrapped(String key, JsonObject value) {
        JsonObject root = new JsonObject();
        root.add(key, value);
        return root;
    }

    private void addVariant(
            JsonObject variants,
            String key,
            String model,
            Integer x,
            Integer y
    ) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", model);
        if (x != null) {
            variant.addProperty("x", x);
        }
        if (y != null) {
            variant.addProperty("y", y);
        }
        variants.add(key, variant);
    }

    private void addMultipart(JsonArray multipart, String facing, String model, Integer y) {
        JsonObject when = new JsonObject();
        when.addProperty("facing", facing);
        JsonObject apply = new JsonObject();
        apply.addProperty("model", model);
        if (y != null) {
            apply.addProperty("y", y);
        }
        JsonObject entry = new JsonObject();
        entry.add("when", when);
        entry.add("apply", apply);
        multipart.add(entry);
    }

    private String bundleId(String tier, String color) {
        return tier + (color == null ? "" : "_" + color) + "_bundle";
    }

    private String mod(String path) {
        return MOD_ID + ":" + path;
    }

    private void save(
            CachedOutput output,
            List<CompletableFuture<?>> writes,
            String path,
            JsonObject json
    ) {
        writes.add(DataProvider.saveStable(output, json, assetsPath.resolve(path + ".json")));
    }
}
