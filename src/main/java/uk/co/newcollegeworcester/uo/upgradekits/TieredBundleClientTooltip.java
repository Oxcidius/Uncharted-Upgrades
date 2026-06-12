package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

import java.util.List;

public final class TieredBundleClientTooltip extends ClientBundleTooltip implements TieredBundleTooltipCapacity {
    private static final int PAGE_SIZE = 12;
    private static final int PAGE_INDICATOR_HEIGHT = 9;

    private final int capacity;
    private final BundleContents fullContents;
    private final int page;
    private final int pageCount;

    public TieredBundleClientTooltip(TieredBundleTooltip tooltip) {
        super(createVisiblePage(tooltip.contents()));
        this.capacity = tooltip.capacity();
        this.fullContents = tooltip.contents();
        this.pageCount = Math.max(1, (tooltip.contents().size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int selected = tooltip.contents().getSelectedItem();
        this.page = selected < 0 ? 0 : selected / PAGE_SIZE;
    }

    @Override
    public int uncharted_upgrades$capacity() {
        return capacity;
    }

    @Override
    public BundleContents uncharted_upgrades$fullContents() {
        return fullContents;
    }

    @Override
    public int getHeight(Font font) {
        return super.getHeight(font) + (pageCount > 1 ? PAGE_INDICATOR_HEIGHT : 0);
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics graphics) {
        super.renderImage(font, x, y, width, height, graphics);
        if (pageCount > 1) {
            Component indicator = Component.literal((page + 1) + "/" + pageCount);
            graphics.drawCenteredString(font, indicator, x + getWidth(font) / 2, y + super.getHeight(font) + 1, 0xFFFFFFFF);
        }
    }

    private static BundleContents createVisiblePage(BundleContents contents) {
        if (contents.size() <= PAGE_SIZE) {
            return contents;
        }

        int selected = contents.getSelectedItem();
        int pageStart = selected < 0 ? 0 : (selected / PAGE_SIZE) * PAGE_SIZE;
        List<ItemStack> allItems = contents.itemCopyStream().toList();
        List<ItemStack> pageItems = allItems.subList(pageStart, Math.min(pageStart + PAGE_SIZE, allItems.size()));
        BundleContents page = new BundleContents(pageItems);

        if (selected >= pageStart && selected < pageStart + pageItems.size()) {
            BundleContents.Mutable mutable = new BundleContents.Mutable(page);
            mutable.toggleSelectedItem(selected - pageStart);
            return mutable.toImmutable();
        }
        return page;
    }
}
