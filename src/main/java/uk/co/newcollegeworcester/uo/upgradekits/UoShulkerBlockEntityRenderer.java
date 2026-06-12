package uk.co.newcollegeworcester.uo.upgradekits;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;

public final class UoShulkerBlockEntityRenderer implements BlockEntityRenderer<UoShulkerBlockEntity, UoShulkerRenderState> {
    private static final EnumMap<UpgradeTier, EnumMap<UoShulkerVariant, Material>> MATERIALS =
            createMaterials();
    private final ShulkerBoxRenderer renderer;

    public UoShulkerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        renderer = new ShulkerBoxRenderer(context);
    }

    @Override
    public UoShulkerRenderState createRenderState() {
        return new UoShulkerRenderState();
    }

    @Override
    public void extractRenderState(
            UoShulkerBlockEntity blockEntity,
            UoShulkerRenderState state,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        state.direction = blockEntity.getBlockState().getValue(ShulkerBoxBlock.FACING);
        state.progress = blockEntity.getProgress(partialTick);
        state.material = MATERIALS.get(blockEntity.getTier()).get(blockEntity.getVariant());
    }

    @Override
    public void submit(UoShulkerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        renderer.submit(
                poseStack,
                collector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.direction,
                state.progress,
                state.breakProgress,
                state.material,
                0
        );
    }

    private static EnumMap<UpgradeTier, EnumMap<UoShulkerVariant, Material>> createMaterials() {
        EnumMap<UpgradeTier, EnumMap<UoShulkerVariant, Material>> materials =
                new EnumMap<>(UpgradeTier.class);
        for (UpgradeTier tier : UpgradeTier.values()) {
            EnumMap<UoShulkerVariant, Material> tierMaterials =
                    new EnumMap<>(UoShulkerVariant.class);
            for (UoShulkerVariant variant : UoShulkerVariant.values()) {
                tierMaterials.put(variant, new Material(
                        Sheets.SHULKER_SHEET,
                        Identifier.fromNamespaceAndPath(
                                UoUpgradeKits.MOD_ID,
                                "entity/shulker/" + variant.textureId(tier)
                        )
                ));
            }
            materials.put(tier, tierMaterials);
        }
        return materials;
    }
}
