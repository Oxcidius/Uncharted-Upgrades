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

final class UoAdvancementProvider implements DataProvider {
    private static final String MOD_ID = "uncharted_upgrades";
    private static final String[] TIERS = {"copper", "iron", "gold", "diamond", "netherite"};

    private final Path advancementPath;

    UoAdvancementProvider(FabricPackOutput output) {
        this.advancementPath = output.getOutputFolder()
                .resolve("data")
                .resolve(MOD_ID)
                .resolve("advancement")
                .resolve("progression");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        save(output, writes, "root", rootAdvancement());
        save(output, writes, "kit_template", inventoryAdvancement(
                mod("progression/root"),
                mod("kit_template"),
                "kit_template",
                "task",
                List.of(mod("kit_template"))
        ));
        save(output, writes, "create_upgrade_kit", inventoryAdvancement(
                mod("progression/kit_template"),
                mod("copper_upgrade_kit"),
                "create_upgrade_kit",
                "task",
                tieredKits("upgrade_kit")
        ));
        save(output, writes, "create_conversion_kit", inventoryAdvancement(
                mod("progression/create_upgrade_kit"),
                mod("copper_conversion_kit"),
                "create_conversion_kit",
                "task",
                tieredKits("conversion_kit")
        ));
        save(output, writes, "upgrade_functional_block", impossibleAdvancement(
                mod("progression/create_upgrade_kit"),
                mod("copper_chest"),
                "upgrade_functional_block",
                "goal",
                "used_upgrade_kit_on_block"
        ));
        save(output, writes, "upgrade_functional_item", impossibleAdvancement(
                mod("progression/create_upgrade_kit"),
                mod("copper_bundle"),
                "upgrade_functional_item",
                "goal",
                "used_upgrade_kit_on_item"
        ));
        save(output, writes, "converted_functional_block", impossibleAdvancement(
                mod("progression/create_conversion_kit"),
                mod("iron_chest"),
                "converted_functional_block",
                "goal",
                "used_conversion_kit_on_block"
        ));
        save(output, writes, "converted_functional_item", impossibleAdvancement(
                mod("progression/create_conversion_kit"),
                mod("iron_bundle"),
                "converted_functional_item",
                "goal",
                "used_conversion_kit_on_item"
        ));
        save(output, writes, "max_upgrade", impossibleAdvancement(
                mod("progression/create_conversion_kit"),
                mod("netherite_conversion_kit"),
                "max_upgrade",
                "challenge",
                "used_netherite_conversion"
        ));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Uncharted Upgrades advancements";
    }

    private JsonObject rootAdvancement() {
        JsonObject tickCriterion = new JsonObject();
        tickCriterion.addProperty("trigger", "minecraft:tick");
        JsonObject criteria = new JsonObject();
        criteria.add("entered_world", tickCriterion);
        JsonArray alternatives = new JsonArray();
        alternatives.add("entered_world");
        JsonArray requirements = new JsonArray();
        requirements.add(alternatives);

        JsonObject advancement = new JsonObject();
        advancement.add("criteria", criteria);
        advancement.add("display", display(mod("netherite_conversion_kit"), "root", "task", true));
        advancement.add("requirements", requirements);
        advancement.addProperty("sends_telemetry_event", false);
        return advancement;
    }

    private JsonObject inventoryAdvancement(
            String parent,
            String icon,
            String translationId,
            String frame,
            List<String> acceptedItems
    ) {
        JsonObject advancement = new JsonObject();
        advancement.addProperty("parent", parent);

        JsonObject criteria = new JsonObject();
        JsonArray requirements = new JsonArray();
        JsonArray alternatives = new JsonArray();
        for (int index = 0; index < acceptedItems.size(); index++) {
            String criterionName = "has_item_" + index;
            criteria.add(criterionName, inventoryCriterion(acceptedItems.get(index)));
            alternatives.add(criterionName);
        }
        requirements.add(alternatives);
        advancement.add("criteria", criteria);
        advancement.add("display", display(icon, translationId, frame, false));
        advancement.add("requirements", requirements);
        advancement.addProperty("sends_telemetry_event", false);
        return advancement;
    }

    private JsonObject impossibleAdvancement(
            String parent,
            String icon,
            String translationId,
            String frame,
            String criterionName
    ) {
        JsonObject criteria = new JsonObject();
        criteria.add(criterionName, impossibleCriterion());
        JsonArray alternatives = new JsonArray();
        alternatives.add(criterionName);
        JsonArray requirements = new JsonArray();
        requirements.add(alternatives);

        JsonObject advancement = new JsonObject();
        advancement.addProperty("parent", parent);
        advancement.add("criteria", criteria);
        advancement.add("display", display(icon, translationId, frame, false));
        advancement.add("requirements", requirements);
        advancement.addProperty("sends_telemetry_event", false);
        return advancement;
    }

    private JsonObject impossibleCriterion() {
        JsonObject criterion = new JsonObject();
        criterion.addProperty("trigger", "minecraft:impossible");
        return criterion;
    }

    private JsonObject inventoryCriterion(String item) {
        JsonObject itemPredicate = new JsonObject();
        itemPredicate.addProperty("items", item);
        JsonArray items = new JsonArray();
        items.add(itemPredicate);
        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        JsonObject criterion = new JsonObject();
        criterion.add("conditions", conditions);
        criterion.addProperty("trigger", "minecraft:inventory_changed");
        return criterion;
    }

    private JsonObject display(String icon, String translationId, String frame, boolean root) {
        JsonObject display = new JsonObject();
        display.addProperty("announce_to_chat", !root);
        if (root) {
            display.addProperty("background", "minecraft:gui/advancements/backgrounds/stone");
        }
        display.add("description", translation("advancements." + MOD_ID + "." + translationId + ".description"));
        JsonObject iconObject = new JsonObject();
        iconObject.addProperty("count", 1);
        iconObject.addProperty("id", icon);
        display.add("icon", iconObject);
        display.addProperty("frame", frame);
        display.addProperty("show_toast", !root);
        display.add("title", translation("advancements." + MOD_ID + "." + translationId + ".title"));
        return display;
    }

    private JsonObject translation(String key) {
        JsonObject translation = new JsonObject();
        translation.addProperty("translate", key);
        return translation;
    }

    private List<String> tieredKits(String suffix) {
        List<String> kits = new ArrayList<>();
        for (String tier : TIERS) {
            kits.add(mod(tier + "_" + suffix));
        }
        return kits;
    }

    private String mod(String path) {
        return MOD_ID + ":" + path;
    }

    private void save(
            CachedOutput output,
            List<CompletableFuture<?>> writes,
            String id,
            JsonObject json
    ) {
        writes.add(DataProvider.saveStable(output, json, advancementPath.resolve(id + ".json")));
    }
}
