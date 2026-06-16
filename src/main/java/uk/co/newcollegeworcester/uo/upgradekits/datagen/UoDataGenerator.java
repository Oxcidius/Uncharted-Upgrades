package uk.co.newcollegeworcester.uo.upgradekits.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class UoDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(UoRecipeProvider::new);
        pack.addProvider(UoTagProvider::new);
        pack.addProvider(UoLootTableProvider::new);
        pack.addProvider(UoModelProvider::new);
        pack.addProvider(UoAdvancementProvider::new);
    }
}
