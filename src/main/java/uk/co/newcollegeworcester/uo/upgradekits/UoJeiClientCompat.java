package uk.co.newcollegeworcester.uo.upgradekits;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

public final class UoJeiClientCompat implements IModPlugin {
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(UoUpgradeKits.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(UoChestScreen.class, new UoChestGuiHandler());
    }

    private static final class UoChestGuiHandler implements IGuiContainerHandler<UoChestScreen> {
        @Override
        public List<Rect2i> getGuiExtraAreas(UoChestScreen screen) {
            return UnchartedUpgradesClientApi.getExtraAreas(screen);
        }
    }
}
