package uk.co.newcollegeworcester.uo.upgradekits;

import java.util.ArrayList;
import java.util.List;

import net.blay09.mods.trashslot.api.SlotRenderStyle;
import net.blay09.mods.trashslot.client.gui.layout.SimpleGuiContainerLayout;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class UoTrashSlotLayout extends SimpleGuiContainerLayout {
    private static final int SCROLLBAR_COLLISION_PADDING = 0;
    private static final int TRASH_SLOT_Y_OFFSET = -1;

    public UoTrashSlotLayout() {
        enableDefaultCollision();
        enableDefaultSnaps();
        setEnabledByDefault();
    }

    @Override
    public List<Rect2i> getCollisionAreas(AbstractContainerScreen<?> screen) {
        List<Rect2i> areas = new ArrayList<>(super.getCollisionAreas(screen));
        if (screen instanceof UoChestScreen uoChestScreen && uoChestScreen.hasScrollbar()) {
            areas.add(new Rect2i(
                uoChestScreen.getScrollbarX(),
                uoChestScreen.getScrollbarY(),
                uoChestScreen.getScrollbarWidth() + SCROLLBAR_COLLISION_PADDING,
                uoChestScreen.getScrollbarHeight()
            ));
        }
        return areas;
    }

    @Override
    public int getSlotOffsetY(AbstractContainerScreen<?> screen, SlotRenderStyle slotRenderStyle) {
        return super.getSlotOffsetY(screen, slotRenderStyle) + TRASH_SLOT_Y_OFFSET;
    }

    @Override
    public String getContainerId(AbstractContainerScreen<?> screen) {
        if (screen instanceof UoChestScreen uoChestScreen) {
            return "uo_chest_" + uoChestScreen.getMenu().slots.size();
        }
        return super.getContainerId(screen);
    }
}
