package uk.co.newcollegeworcester.uo.upgradekits;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UoTrashSlotCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(UoTrashSlotCompat.class);

    private UoTrashSlotCompat() {
    }

    public static void register() {
        try {
            Class<?> apiClass = Class.forName("net.blay09.mods.trashslot.api.TrashSlotAPI");
            Class<?> layoutClass = Class.forName("net.blay09.mods.trashslot.api.layout.TrashContainerLayout");
            Object defaultLayout = apiClass.getMethod("getDefaultLayout").invoke(null);
            Object layout = Proxy.newProxyInstance(
                    UoTrashSlotCompat.class.getClassLoader(),
                    new Class<?>[]{layoutClass},
                    new UoTrashSlotLayout(defaultLayout)
            );
            Method registerLayout = apiClass.getMethod(
                    "registerLayout",
                    net.minecraft.world.inventory.MenuType.class,
                    layoutClass
            );
            registerLayout.invoke(null, UoRegistries.chestMenu(), layout);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Could not register TrashSlot layout compatibility", exception);
        }
    }
}
