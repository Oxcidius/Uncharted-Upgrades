package uk.co.newcollegeworcester.uo.upgradekits;

import java.util.Collection;
import java.util.List;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;

public final class UoReiClientCompat implements REIClientPlugin {
    private static final int EXCLUSION_PADDING = 0;

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(UoChestScreen.class, UoReiClientCompat::scrollbarZone);
    }

    private static Collection<Rectangle> scrollbarZone(UoChestScreen screen) {
        if (!screen.hasScrollbar()) {
            return List.of();
        }
        return List.of(new Rectangle(
            screen.getScrollbarX(),
            screen.getScrollbarY(),
            screen.getScrollbarWidth() + EXCLUSION_PADDING,
            screen.getScrollbarHeight()
        ));
    }
}
