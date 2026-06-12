package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class UoChestRenderState extends BlockEntityRenderState {
    public float angle;
    public float open;
    public Material material;
    public ChestType type = ChestType.SINGLE;
}
