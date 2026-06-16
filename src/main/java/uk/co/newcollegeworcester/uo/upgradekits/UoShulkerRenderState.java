package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;

public final class UoShulkerRenderState extends BlockEntityRenderState {
    public Direction direction = Direction.UP;
    public float progress;
    public SpriteId sprite;
}
