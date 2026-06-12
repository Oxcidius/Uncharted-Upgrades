package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BundleContents;

public record TieredBundleTooltip(BundleContents contents, int capacity) implements TooltipComponent {
}
