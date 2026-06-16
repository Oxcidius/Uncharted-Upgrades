package uk.co.newcollegeworcester.uo.upgradekits;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;

public final class UoShulkerBlockEntityRenderer implements BlockEntityRenderer<UoShulkerBlockEntity, UoShulkerRenderState> {
    private static final EnumMap<UpgradeTier, EnumMap<UoShulkerVariant, SpriteId>> SPRITES =
            createSprites();
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
        state.sprite = SPRITES.get(blockEntity.getTier()).get(blockEntity.getVariant());
    }

    @Override
    public void submit(UoShulkerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(ShulkerBoxRenderer.modelTransform(state.direction));
        renderer.submit(
                poseStack,
                collector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.progress,
                state.breakProgress,
                state.sprite,
                0
        );
        poseStack.popPose();
    }

    private static EnumMap<UpgradeTier, EnumMap<UoShulkerVariant, SpriteId>> createSprites() {
        EnumMap<UpgradeTier, EnumMap<UoShulkerVariant, SpriteId>> sprites =
                new EnumMap<>(UpgradeTier.class);
        for (UpgradeTier tier : UpgradeTier.values()) {
            EnumMap<UoShulkerVariant, SpriteId> tierSprites =
                    new EnumMap<>(UoShulkerVariant.class);
            for (UoShulkerVariant variant : UoShulkerVariant.values()) {
                tierSprites.put(variant, new SpriteId(
                        Sheets.SHULKER_SHEET,
                        Identifier.fromNamespaceAndPath(
                                UoUpgradeKits.MOD_ID,
                                "entity/shulker/" + variant.textureId(tier)
                        )
                ));
            }
            sprites.put(tier, tierSprites);
        }
        return sprites;
    }
}
