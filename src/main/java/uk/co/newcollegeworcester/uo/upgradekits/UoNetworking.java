package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

final class UoNetworking {
    private UoNetworking() {
    }

    static void register() {
        PayloadTypeRegistry.serverboundPlay().register(UoChestActionPayload.TYPE, UoChestActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UoChestActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof UoChestMenu menu) {
                        menu.handleButtonAction(context.player(), payload.action());
                    }
                })
        );
    }
}
