package uk.co.newcollegeworcester.uo.upgradekits;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;

public final class UoChestBlockEntityRenderer implements BlockEntityRenderer<UoChestBlockEntity, UoChestRenderState> {
    private static final EnumMap<UpgradeTier, EnumMap<ChestType, SpriteId>> SPRITES = createSprites();
    private final SpriteGetter sprites;
    private final ChestModel singleModel;
    private final ChestModel leftModel;
    private final ChestModel rightModel;

    public UoChestBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.singleModel = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        this.leftModel = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        this.rightModel = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
    }

    @Override
    public UoChestRenderState createRenderState() {
        return new UoChestRenderState();
    }

    @Override
    public void extractRenderState(UoChestBlockEntity blockEntity, UoChestRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        BlockState blockState = blockEntity.getBlockState();
        Direction direction = blockState.hasProperty(UoChestBlock.FACING) ? blockState.getValue(UoChestBlock.FACING) : Direction.SOUTH;
        ChestType type = blockState.hasProperty(UoChestBlock.TYPE) ? blockState.getValue(UoChestBlock.TYPE) : ChestType.SINGLE;
        state.angle = direction.toYRot();
        state.open = blockEntity.getOpenNess(partialTick);
        state.type = type;
        state.sprite = spriteFor(blockEntity.getTier(), type);
    }

    @Override
    public void submit(UoChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.angle));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        float open = 1.0F - state.open;
        open = 1.0F - open * open * open;
        collector.submitModel(
                modelFor(state.type),
                open,
                poseStack,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                state.sprite,
                sprites,
                0,
                state.breakProgress
        );
        poseStack.popPose();
    }

    private ChestModel modelFor(ChestType type) {
        if (type == ChestType.LEFT) {
            return leftModel;
        }
        if (type == ChestType.RIGHT) {
            return rightModel;
        }
        return singleModel;
    }

    private static SpriteId spriteFor(UpgradeTier tier, ChestType type) {
        return SPRITES.get(tier).get(type);
    }

    private static EnumMap<UpgradeTier, EnumMap<ChestType, SpriteId>> createSprites() {
        EnumMap<UpgradeTier, EnumMap<ChestType, SpriteId>> sprites =
                new EnumMap<>(UpgradeTier.class);
        for (UpgradeTier tier : UpgradeTier.values()) {
            EnumMap<ChestType, SpriteId> tierSprites = new EnumMap<>(ChestType.class);
            for (ChestType type : ChestType.values()) {
                String variant = switch (type) {
                    case LEFT -> "left";
                    case RIGHT -> "right";
                    case SINGLE -> "single";
                };
                tierSprites.put(type, new SpriteId(
                        Sheets.CHEST_SHEET,
                        Identifier.fromNamespaceAndPath(
                                UoUpgradeKits.MOD_ID,
                                "entity/chest/" + tier.id + "/" + variant
                        )
                ));
            }
            sprites.put(tier, tierSprites);
        }
        return sprites;
    }
}
