package uk.co.newcollegeworcester.uo.upgradekits;

import net.blay09.mods.trashslot.api.TrashSlotAPI;

public final class UoTrashSlotCompat {
    private UoTrashSlotCompat() {
    }

    public static void register() {
        TrashSlotAPI.registerLayout(UoChestScreen.class, new UoTrashSlotLayout());
    }
}
