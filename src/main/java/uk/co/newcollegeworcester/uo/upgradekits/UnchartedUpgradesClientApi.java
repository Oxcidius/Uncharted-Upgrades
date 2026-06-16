package uk.co.newcollegeworcester.uo.upgradekits;

import java.util.List;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

public final class UnchartedUpgradesClientApi {
    private UnchartedUpgradesClientApi() {
    }

    public static boolean isStorageScreen(Screen screen) {
        return screen instanceof UoChestScreen;
    }

    public static List<Rect2i> getExtraAreas(Screen screen) {
        if (screen instanceof UoChestScreen uoScreen && uoScreen.hasScrollbar()) {
            return List.of(new Rect2i(
                    uoScreen.getScrollbarX(),
                    uoScreen.getScrollbarY(),
                    uoScreen.getScrollbarWidth(),
                    uoScreen.getScrollbarHeight()
            ));
        }
        return List.of();
    }
}
