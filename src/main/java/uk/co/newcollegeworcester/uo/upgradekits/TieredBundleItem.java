package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public final class TieredBundleItem extends BundleItem {
    private static final ThreadLocal<Fraction> ACTIVE_CAPACITY = new ThreadLocal<>();
    private static final ThreadLocal<PendingCapacity> PENDING_CAPACITY = new ThreadLocal<>();
    private final UpgradeTier tier;

    public TieredBundleItem(UpgradeTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public UpgradeTier getTier() {
        return tier;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    public static Fraction activeCapacity() {
        Fraction capacity = ACTIVE_CAPACITY.get();
        return capacity == null ? Fraction.ONE : capacity;
    }

    public static void rememberCapacity(BundleContents contents, int capacity) {
        PENDING_CAPACITY.set(new PendingCapacity(
                new WeakReference<>(contents),
                Fraction.getFraction(capacity, 1)
        ));
    }

    public static Fraction consumeCapacity(BundleContents contents) {
        PendingCapacity pending = PENDING_CAPACITY.get();
        PENDING_CAPACITY.remove();
        if (pending == null || pending.contents().get() != contents) {
            return Fraction.ONE;
        }
        return pending.capacity();
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack bundle, Slot slot, ClickAction action, Player player) {
        return withCapacity(() -> super.overrideStackedOnOther(bundle, slot, action, player));
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack bundle,
            ItemStack carried,
            Slot slot,
            ClickAction action,
            Player player,
            SlotAccess carriedAccess
    ) {
        return withCapacity(() -> super.overrideOtherStackedOnMe(bundle, carried, slot, action, player, carriedAccess));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        float fullness = contents.weight().getOrThrow().floatValue() / tier.bundleCapacity;
        return Math.min(1 + (int) (fullness * 12.0F), 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        boolean full = contents.weight().getOrThrow().compareTo(Fraction.getFraction(tier.bundleCapacity, 1)) >= 0;
        return full
                ? ARGB.colorFromFloat(1.0F, 1.0F, 0.33F, 0.33F)
                : ARGB.colorFromFloat(1.0F, 0.44F, 0.53F, 1.0F);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return super.getTooltipImage(stack).map(ignored -> new TieredBundleTooltip(
                stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY),
                tier.bundleCapacity
        ));
    }

    private boolean withCapacity(BooleanSupplier action) {
        ACTIVE_CAPACITY.set(Fraction.getFraction(tier.bundleCapacity, 1));
        try {
            return action.getAsBoolean();
        } finally {
            ACTIVE_CAPACITY.remove();
        }
    }

    private record PendingCapacity(WeakReference<BundleContents> contents, Fraction capacity) {
    }
}
