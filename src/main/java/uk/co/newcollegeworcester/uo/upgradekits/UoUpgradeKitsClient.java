package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class UoUpgradeKitsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(UoRegistries.chestMenu(), UoChestScreen::new);
        UoRegistries.chestBlockEntityTypes().forEach(type ->
                BlockEntityRenderers.register(type, UoChestBlockEntityRenderer::new)
        );
        UoRegistries.shulkerBlockEntityTypes().forEach(type ->
                BlockEntityRenderers.register(type, UoShulkerBlockEntityRenderer::new)
        );
        TooltipComponentCallback.EVENT.register(data ->
                data instanceof TieredBundleTooltip tooltip ? new TieredBundleClientTooltip(tooltip) : null
        );
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> addUoTooltip(stack, lines));

        if (FabricLoader.getInstance().isModLoaded("trashslot")) {
            UoTrashSlotCompat.register();
        }
    }

    private static void addUoTooltip(net.minecraft.world.item.ItemStack stack, List<Component> lines) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!UoUpgradeKits.MOD_ID.equals(id.getNamespace())) {
            return;
        }

        String path = id.getPath();
        UpgradeTier tier = UpgradeTier.fromPath(path);
        int insertAt = Math.min(1, lines.size());
        if (tier != null) {
            lines.add(insertAt++, Component.translatable(
                    "tooltip.uncharted_upgrades.tier",
                    Component.translatable("tooltip.uncharted_upgrades.tier." + tier.id)
                            .withStyle(tier.tooltipColor)
            ).withStyle(ChatFormatting.GRAY));
        }
        lines.add(insertAt, Component.translatable(descriptionKey(path)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String descriptionKey(String path) {
        if (path.contains("bundle")) {
            return "tooltip.uncharted_upgrades.description.bundle";
        }
        if (path.contains("chest") || path.contains("barrel") || path.contains("shulker")) {
            return "tooltip.uncharted_upgrades.description.storage";
        }
        if (path.contains("hopper")) {
            return "tooltip.uncharted_upgrades.description.hopper";
        }
        if (path.contains("furnace") || path.contains("smoker")) {
            return "tooltip.uncharted_upgrades.description.cooking";
        }
        return "tooltip.uncharted_upgrades.description.kit";
    }
}
