package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

final class UoCreativeTab {
    private UoCreativeTab() {
    }

    static void register() {
        CreativeModeTab tab = FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.uncharted_upgrades.main"))
                .icon(() -> new ItemStack(UoItems.kitTemplate()))
                .displayItems((parameters, output) -> {
                    output.accept(UoItems.kitTemplate());
                    for (UpgradeTier tier : UpgradeTier.values()) {
                        output.accept(UoItems.upgradeKit(tier));
                        output.accept(UoItems.conversionKit(tier));
                    }
                    for (UpgradeTier tier : UpgradeTier.values()) {
                        for (CookingType type : CookingType.values()) {
                            output.accept(UoBlocks.cooking(tier, type));
                        }
                    }
                    for (UpgradeTier tier : UpgradeTier.values()) {
                        output.accept(UoBlocks.chest(tier));
                    }
                    for (UpgradeTier tier : UpgradeTier.values()) {
                        output.accept(UoBlocks.barrel(tier));
                    }
                    for (UpgradeTier tier : UpgradeTier.values()) {
                        UoBlocks.shulkers(tier).values().forEach(output::accept);
                    }
                    for (UpgradeTier tier : UpgradeTier.values()) {
                        output.accept(UoBlocks.hopper(tier));
                    }
                    for (UpgradeTier tier : UpgradeTier.values()) {
                        UoItems.bundles(tier).forEach(output::accept);
                    }
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, UoRegistries.id("main"), tab);
    }
}
