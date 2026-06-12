package uk.co.newcollegeworcester.uo.upgradekits;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UoChestActionPayload(int action) implements CustomPacketPayload {
    public static final int MOVE_ALL_TO_CHEST = 0;
    public static final int MOVE_ALL_FROM_CHEST = 1;
    public static final Type<UoChestActionPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(UoUpgradeKits.MOD_ID, "chest_action")
    );
    public static final StreamCodec<ByteBuf, UoChestActionPayload> CODEC = ByteBufCodecs.VAR_INT.map(
        UoChestActionPayload::new,
        UoChestActionPayload::action
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
