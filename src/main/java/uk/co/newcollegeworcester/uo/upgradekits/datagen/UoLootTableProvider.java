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

final class UoLootTableProvider implements DataProvider {
    private static final String MOD_ID = "uncharted_upgrades";
    private static final String[] TIERS = {"copper", "iron", "gold", "diamond", "netherite"};
    private static final String[] COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
    private static final String[] STANDARD_BLOCKS = {
            "chest", "barrel", "hopper", "furnace", "smoker", "blast_furnace"
    };

    private final Path lootTablesPath;

    UoLootTableProvider(FabricDataOutput output) {
        this.lootTablesPath = output.getOutputFolder()
                .resolve("data")
                .resolve(MOD_ID)
                .resolve("loot_table")
                .resolve("blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (String tier : TIERS) {
            for (String blockType : STANDARD_BLOCKS) {
                save(output, writes, tier + "_" + blockType, standardBlock(tier + "_" + blockType));
            }

            save(output, writes, tier + "_shulker_box", shulker(tier + "_shulker_box"));
            for (String color : COLORS) {
                String id = color + "_" + tier + "_shulker_box";
                save(output, writes, id, shulker(id));
            }
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Uncharted Upgrades block loot tables";
    }

    private JsonObject standardBlock(String id) {
        JsonObject pool = basePool(id, List.of("minecraft:custom_name"));
        JsonArray conditions = new JsonArray();
        JsonObject survivesExplosion = new JsonObject();
        survivesExplosion.addProperty("condition", "minecraft:survives_explosion");
        conditions.add(survivesExplosion);
        pool.add("conditions", conditions);
        return lootTable(pool);
    }

    private JsonObject shulker(String id) {
        return lootTable(basePool(id, List.of(
                "minecraft:custom_name",
                "minecraft:container",
                "minecraft:lock",
                "minecraft:container_loot"
        )));
    }

    private JsonObject basePool(String id, List<String> copiedComponents) {
        JsonObject copyComponents = new JsonObject();
        copyComponents.addProperty("function", "minecraft:copy_components");
        JsonArray include = new JsonArray();
        copiedComponents.forEach(include::add);
        copyComponents.add("include", include);
        copyComponents.addProperty("source", "block_entity");

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        JsonArray functions = new JsonArray();
        functions.add(copyComponents);
        entry.add("functions", functions);
        entry.addProperty("name", MOD_ID + ":" + id);

        JsonObject pool = new JsonObject();
        pool.addProperty("bonus_rolls", 0.0D);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        pool.addProperty("rolls", 1.0D);
        return pool;
    }

    private JsonObject lootTable(JsonObject pool) {
        JsonObject lootTable = new JsonObject();
        lootTable.addProperty("type", "minecraft:block");
        JsonArray pools = new JsonArray();
        pools.add(pool);
        lootTable.add("pools", pools);
        return lootTable;
    }

    private void save(
            CachedOutput output,
            List<CompletableFuture<?>> writes,
            String id,
            JsonObject json
    ) {
        writes.add(DataProvider.saveStable(output, json, lootTablesPath.resolve(id + ".json")));
    }
}
