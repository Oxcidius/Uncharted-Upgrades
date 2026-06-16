package uk.co.newcollegeworcester.uo.upgradekits;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UoTrashSlotLayout implements InvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UoTrashSlotLayout.class);
    private static final Identifier SCROLLBAR_BOUNDS =
            Identifier.fromNamespaceAndPath(UoUpgradeKits.MOD_ID, "scrollbar");

    private final Object delegate;

    UoTrashSlotLayout(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("getAllBounds".equals(method.getName())) {
            @SuppressWarnings("unchecked")
            List<Rect2i> delegateBounds = (List<Rect2i>) invokeDelegate(method, args);
            List<Rect2i> bounds = new ArrayList<>(delegateBounds);
            scrollbarBounds(args[0]).ifPresent(bounds::add);
            return bounds;
        }
        if ("getBounds".equals(method.getName()) && SCROLLBAR_BOUNDS.equals(args[1])) {
            return scrollbarBounds(args[0]);
        }
        return invokeDelegate(method, args);
    }

    private Object invokeDelegate(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static Optional<Rect2i> scrollbarBounds(Object context) {
        try {
            Object screen = context.getClass().getMethod("screen").invoke(context);
            if (screen instanceof UoChestScreen uoScreen && uoScreen.hasScrollbar()) {
                return Optional.of(new Rect2i(
                        uoScreen.getScrollbarX(),
                        uoScreen.getScrollbarY(),
                        uoScreen.getScrollbarWidth(),
                        uoScreen.getScrollbarHeight()
                ));
            }
        } catch (ReflectiveOperationException exception) {
            LOGGER.debug("Could not determine TrashSlot scrollbar bounds", exception);
        }
        return Optional.empty();
    }
}
