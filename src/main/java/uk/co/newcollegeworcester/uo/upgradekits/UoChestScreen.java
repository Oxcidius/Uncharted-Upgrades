package uk.co.newcollegeworcester.uo.upgradekits;

import java.util.ArrayList;
import java.util.Locale;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class UoChestScreen extends AbstractContainerScreen<UoChestMenu> {
    private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier CREATIVE_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/creative_inventory/tab_items.png");
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    private static final Identifier MOVE_TO_CHEST_GLYPH = Identifier.fromNamespaceAndPath(UoUpgradeKits.MOD_ID, "textures/gui/buttons/move_to_chest.png");
    private static final Identifier MOVE_FROM_CHEST_GLYPH = Identifier.fromNamespaceAndPath(UoUpgradeKits.MOD_ID, "textures/gui/buttons/move_from_chest.png");
    private static final Identifier SEARCH_GLYPH = Identifier.fromNamespaceAndPath(UoUpgradeKits.MOD_ID, "textures/gui/buttons/search.png");
    private static final int COLUMNS = 9;
    private static final int HIDDEN_Y = -10000;
    private static final int LABEL_COLOR = 0xff404040;
    private static final int SCROLLBAR_SIDEBAR_GAP = -3;
    private static final int SCROLLBAR_SIDEBAR_WIDTH = 18;
    private static final int SCROLLBAR_SIDEBAR_PADDING = 3;
    private static final int SCROLLBAR_SIDEBAR_RIGHT_BEZEL = 3;
    private static final int SCROLLBAR_WIDTH = 14;
    private static final int SCROLL_KNOB_WIDTH = 12;
    private static final int SCROLL_KNOB_HEIGHT = 15;
    private static final int CREATIVE_TRACK_TEXTURE_X = 174;
    private static final int CREATIVE_TRACK_TOP_TEXTURE_Y = 17;
    private static final int CREATIVE_TRACK_MIDDLE_TEXTURE_Y = 20;
    private static final int CREATIVE_TRACK_BOTTOM_TEXTURE_Y = 129;
    private static final int CREATIVE_TRACK_EDGE_HEIGHT = 3;
    private static final int CONTAINER_TEXTURE_SIZE = 256;
    private static final int SIDEBAR_TEXTURE_X = 158;
    private static final int SIDEBAR_EDGE_HEIGHT = 4;
    private static final int SIDEBAR_MIDDLE_TEXTURE_Y = 4;
    private static final int SIDEBAR_MIDDLE_TILE_HEIGHT = 12;
    private static final int SIDEBAR_BOTTOM_TEXTURE_Y = 218;
    private static final int HEADER_BUTTON_SIZE = 12;
    private static final int HEADER_BUTTON_SPACING = 18;
    private static final int HEADER_GLYPH_SIZE = 10;
    private static final int WRAPPED_TITLE_EXTRA_HEIGHT = 9;
    private static final int TITLE_BUTTON_GAP = 4;
    private static final String CLIENTSORT_TRIGGER_BUTTON_CLASS = "dev.terminalmc.clientsort.client.gui.widget.TriggerButton";
    private int scrollRow;
    private boolean scrolling;
    private EditBox searchBox;
    private String activeSearch = "";
    private boolean focusSearchBox;
    private int titleHeaderOffset;
    private int checkedChildCount = -1;

    public UoChestScreen(UoChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 114 + menu.getVisibleRows() * 18;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = menu.getPlayerInventoryY() - 11;
    }

    @Override
    protected void init() {
        int titleWidth = 8 + (COLUMNS - 3) * HEADER_BUTTON_SPACING - TITLE_BUTTON_GAP;
        titleHeaderOffset = Minecraft.getInstance().font.width(title) > titleWidth ? WRAPPED_TITLE_EXTRA_HEIGHT : 0;
        imageHeight = 114 + menu.getVisibleRows() * 18 + titleHeaderOffset;
        inventoryLabelY = menu.getPlayerInventoryY() - 11 + titleHeaderOffset;
        super.init();
        menu.setScrollRow(scrollRow, titleHeaderOffset);
        addHeaderButtons();
        addSearchBox();
        suppressClientSortButtonsIfChanged();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        suppressClientSortButtonsIfChanged();
        focusSearchBoxIfRequested();
        applyChestSlotLayout();
        super.render(graphics, mouseX, mouseY, partialTick);
        drawScrollbar(graphics, getScrollbarTrackX(), getScrollbarTrackY());
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (getMaxScrollRow() > 0) {
            setScrollRow(scrollRow - (int) Math.signum(scrollY));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isInScrollbar(event.x(), event.y())) {
            scrolling = true;
            setScrollFromMouse(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (scrolling) {
            setScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scrolling) {
            scrolling = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchBox != null && searchBox.visible && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeSearch();
            return true;
        }
        if (searchBox != null && searchBox.visible && searchBox.isFocused()) {
            return searchBox.keyPressed(event) || true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBox != null && searchBox.visible && searchBox.isFocused()) {
            return searchBox.charTyped(event) || true;
        }
        return super.charTyped(event);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int chestHeight = menu.getVisibleRows() * 18 + 17;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y + titleHeaderOffset, 0.0F, 0.0F, 176, chestHeight, 256, 256);
        if (titleHeaderOffset > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y, 0.0F, 0.0F, 176, 17, 256, 256);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, x, y + titleHeaderOffset + chestHeight, 0.0F, 126.0F, 176, 96, 256, 256);
        drawScrollbarSidebar(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (searchBox == null || !searchBox.visible) {
            int titleWidth = getHeaderButtonsX() - leftPos - titleLabelX - TITLE_BUTTON_GAP;
            var titleLines = font.split(title, titleWidth);
            int lineCount = Math.min(titleHeaderOffset > 0 ? 2 : 1, titleLines.size());
            for (int line = 0; line < lineCount; line++) {
                graphics.drawString(font, titleLines.get(line), titleLabelX, titleLabelY + line * 9, LABEL_COLOR, false);
            }
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    private void addHeaderButtons() {
        int x = getHeaderButtonsX();
        int y = topPos + 3;
        Button moveToChestButton = new GlyphButton(
            x,
            y,
            MOVE_TO_CHEST_GLYPH,
            Component.literal("Move inventory to chest"),
            button -> {
                ClientPlayNetworking.send(new UoChestActionPayload(UoChestActionPayload.MOVE_ALL_TO_CHEST));
                clearButtonFocus(button);
            }
        );
        moveToChestButton.setOverrideRenderHighlightedSprite(moveToChestButton::isHovered);
        addRenderableWidget(moveToChestButton);

        Button moveFromChestButton = new GlyphButton(
            x + HEADER_BUTTON_SPACING,
            y,
            MOVE_FROM_CHEST_GLYPH,
            Component.literal("Move chest to inventory"),
            button -> {
                ClientPlayNetworking.send(new UoChestActionPayload(UoChestActionPayload.MOVE_ALL_FROM_CHEST));
                clearButtonFocus(button);
            }
        );
        moveFromChestButton.setOverrideRenderHighlightedSprite(moveFromChestButton::isHovered);
        addRenderableWidget(moveFromChestButton);

        Button searchButton = new GlyphButton(
            x + HEADER_BUTTON_SPACING * 2,
            y,
            SEARCH_GLYPH,
            Component.literal("Search chest"),
            button -> {
                toggleSearch();
                if (searchBox == null || !searchBox.visible) {
                    clearButtonFocus(button);
                }
            }
        );
        searchButton.setOverrideRenderHighlightedSprite(() -> searchButton.isHovered() || searchBox != null && searchBox.visible);
        addRenderableWidget(searchButton);
    }

    private void clearButtonFocus(Button button) {
        button.setFocused(false);
        if (getFocused() == button) {
            setFocused(null);
        }
    }

    private void addSearchBox() {
        int width = Math.max(45, getHeaderButtonsX() - leftPos - 12);
        searchBox = new EditBox(font, leftPos + 7, topPos + 5, width, 12, Component.literal("Search"));
        searchBox.setBordered(false);
        searchBox.setTextColor(LABEL_COLOR);
        searchBox.setHint(Component.literal("Search"));
        searchBox.setVisible(false);
        searchBox.setResponder(value -> {
            scrollRow = 0;
            activeSearch = value;
            applyChestSlotLayout();
        });
        addRenderableWidget(searchBox);
    }

    private int getHeaderButtonsX() {
        return leftPos + 8 + (COLUMNS - 3) * HEADER_BUTTON_SPACING + (HEADER_BUTTON_SPACING - HEADER_BUTTON_SIZE) / 2;
    }

    private void toggleSearch() {
        if (searchBox == null) {
            return;
        }
        boolean visible = !searchBox.visible;
        searchBox.setVisible(visible);
        searchBox.setFocused(visible);
        setFocused(visible ? searchBox : null);
        focusSearchBox = visible;
        if (!visible) {
            searchBox.setValue("");
            activeSearch = "";
        }
        applyChestSlotLayout();
    }

    private void closeSearch() {
        if (searchBox == null) {
            return;
        }
        searchBox.setValue("");
        searchBox.setVisible(false);
        searchBox.setFocused(false);
        activeSearch = "";
        focusSearchBox = false;
        if (getFocused() == searchBox) {
            setFocused(null);
        }
        applyChestSlotLayout();
    }

    private void focusSearchBoxIfRequested() {
        if (!focusSearchBox || searchBox == null || !searchBox.visible) {
            return;
        }
        searchBox.setFocused(true);
        searchBox.moveCursorToEnd(false);
        setFocused(searchBox);
        focusSearchBox = false;
    }

    private void applyChestSlotLayout() {
        if (activeSearch.isBlank()) {
            setUnfilteredScrollRow(scrollRow);
            return;
        }

        ArrayList<Integer> matchingSlots = getMatchingStorageSlots();
        scrollRow = Math.max(0, Math.min(scrollRow, getMaxScrollRow(matchingSlots.size())));
        for (int index = 0; index < menu.getStorageSlots(); index++) {
            Slot slot = menu.slots.get(index);
            ((MutableSlotPosition) slot).uncharted_upgrades$setPosition(8 + (index % COLUMNS) * 18, HIDDEN_Y);
        }

        int firstVisible = scrollRow * COLUMNS;
        int visibleSlots = menu.getVisibleRows() * COLUMNS;
        for (int visibleIndex = 0; visibleIndex < visibleSlots && firstVisible + visibleIndex < matchingSlots.size(); visibleIndex++) {
            Slot slot = menu.slots.get(matchingSlots.get(firstVisible + visibleIndex));
            int row = visibleIndex / COLUMNS;
            int column = visibleIndex % COLUMNS;
            ((MutableSlotPosition) slot).uncharted_upgrades$setPosition(8 + column * 18, 18 + titleHeaderOffset + row * 18);
        }
        menu.setPlayerInventoryOffset(titleHeaderOffset);
    }

    private void setUnfilteredScrollRow(int row) {
        scrollRow = Math.max(0, Math.min(row, menu.getMaxScrollRow()));
        menu.setScrollRow(scrollRow, titleHeaderOffset);
    }

    private ArrayList<Integer> getMatchingStorageSlots() {
        ArrayList<Integer> matches = new ArrayList<>();
        String query = activeSearch.toLowerCase(Locale.ROOT);
        for (int index = 0; index < menu.getStorageSlots(); index++) {
            ItemStack stack = menu.slots.get(index).getItem();
            if (!stack.isEmpty() && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                matches.add(index);
            }
        }
        return matches;
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y) {
        if (getMaxScrollRow() <= 0) {
            return;
        }
        int height = getScrollbarTrackHeight();
        drawCreativeScrollbarTrack(graphics, x, y, height);

        int knobY = scrollbarKnobY(y, height);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, x + 1, knobY, SCROLL_KNOB_WIDTH, SCROLL_KNOB_HEIGHT);
    }

    private boolean isInScrollbar(double mouseX, double mouseY) {
        if (getMaxScrollRow() <= 0) {
            return false;
        }
        return mouseX >= getScrollbarX() && mouseX < getScrollbarX() + getScrollbarWidth()
            && mouseY >= getScrollbarY() && mouseY < getScrollbarY() + getScrollbarHeight();
    }

    private void setScrollFromMouse(double mouseY) {
        int trackY = getScrollbarTrackY();
        int trackHeight = getScrollbarTrackHeight();
        int travel = Math.max(1, trackHeight - SCROLL_KNOB_HEIGHT - 2);
        double position = (mouseY - trackY - 1 - SCROLL_KNOB_HEIGHT / 2.0D) / travel;
        setScrollRow((int) Math.round(position * getMaxScrollRow()));
    }

    private void setScrollRow(int row) {
        scrollRow = Math.max(0, Math.min(getMaxScrollRow(), row));
        applyChestSlotLayout();
    }

    private int scrollbarKnobY(int trackY, int trackHeight) {
        int travel = Math.max(0, trackHeight - SCROLL_KNOB_HEIGHT - 2);
        return trackY + 1 + Math.round((scrollRow / (float) getMaxScrollRow()) * travel);
    }

    private int getMaxScrollRow() {
        if (activeSearch.isBlank()) {
            return menu.getMaxScrollRow();
        }
        return getMaxScrollRow(getMatchingStorageSlots().size());
    }

    private int getMaxScrollRow(int storageSlots) {
        int rows = (int) Math.ceil(storageSlots / (double) COLUMNS);
        return Math.max(0, rows - menu.getVisibleRows());
    }

    public int getScrollbarX() {
        return leftPos + imageWidth + SCROLLBAR_SIDEBAR_GAP;
    }

    public int getScrollbarY() {
        return topPos;
    }

    public int getScrollbarHeight() {
        return menu.getVisibleRows() * 18 + 31 + titleHeaderOffset;
    }

    public int getScrollbarWidth() {
        return SCROLLBAR_SIDEBAR_WIDTH;
    }

    public boolean hasScrollbar() {
        return getMaxScrollRow() > 0;
    }

    private int getScrollbarTrackX() {
        int usableWidth = SCROLLBAR_SIDEBAR_WIDTH - SCROLLBAR_SIDEBAR_RIGHT_BEZEL;
        return getScrollbarX() + (usableWidth - SCROLLBAR_WIDTH) / 2;
    }

    private void drawCreativeScrollbarTrack(GuiGraphics graphics, int x, int y, int height) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CREATIVE_BACKGROUND,
            x,
            y,
            CREATIVE_TRACK_TEXTURE_X,
            CREATIVE_TRACK_TOP_TEXTURE_Y,
            SCROLLBAR_WIDTH,
            CREATIVE_TRACK_EDGE_HEIGHT,
            CONTAINER_TEXTURE_SIZE,
            CONTAINER_TEXTURE_SIZE
        );

        int middleHeight = height - CREATIVE_TRACK_EDGE_HEIGHT * 2;
        for (int offset = 0; offset < middleHeight; offset++) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CREATIVE_BACKGROUND,
                x,
                y + CREATIVE_TRACK_EDGE_HEIGHT + offset,
                CREATIVE_TRACK_TEXTURE_X,
                CREATIVE_TRACK_MIDDLE_TEXTURE_Y,
                SCROLLBAR_WIDTH,
                1,
                CONTAINER_TEXTURE_SIZE,
                CONTAINER_TEXTURE_SIZE
            );
        }

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CREATIVE_BACKGROUND,
            x,
            y + height - CREATIVE_TRACK_EDGE_HEIGHT,
            CREATIVE_TRACK_TEXTURE_X,
            CREATIVE_TRACK_BOTTOM_TEXTURE_Y,
            SCROLLBAR_WIDTH,
            CREATIVE_TRACK_EDGE_HEIGHT,
            CONTAINER_TEXTURE_SIZE,
            CONTAINER_TEXTURE_SIZE
        );
    }

    private int getScrollbarTrackY() {
        return topPos + 18 + titleHeaderOffset + SCROLLBAR_SIDEBAR_PADDING;
    }

    private int getScrollbarTrackHeight() {
        return menu.getVisibleRows() * 18 - SCROLLBAR_SIDEBAR_PADDING * 2;
    }

    private void drawScrollbarSidebar(GuiGraphics graphics) {
        if (!hasScrollbar()) {
            return;
        }
        int x = getScrollbarX();
        int y = getScrollbarY();
        int width = getScrollbarWidth();
        int height = getScrollbarHeight();
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CONTAINER_BACKGROUND,
            x,
            y,
            SIDEBAR_TEXTURE_X,
            0.0F,
            width,
            SIDEBAR_EDGE_HEIGHT,
            CONTAINER_TEXTURE_SIZE,
            CONTAINER_TEXTURE_SIZE
        );

        int middleHeight = height - SIDEBAR_EDGE_HEIGHT * 2;
        for (int offset = 0; offset < middleHeight; offset += SIDEBAR_MIDDLE_TILE_HEIGHT) {
            int tileHeight = Math.min(SIDEBAR_MIDDLE_TILE_HEIGHT, middleHeight - offset);
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CONTAINER_BACKGROUND,
                x,
                y + SIDEBAR_EDGE_HEIGHT + offset,
                SIDEBAR_TEXTURE_X,
                SIDEBAR_MIDDLE_TEXTURE_Y,
                width,
                tileHeight,
                CONTAINER_TEXTURE_SIZE,
                CONTAINER_TEXTURE_SIZE
            );
        }

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CONTAINER_BACKGROUND,
            x,
            y + height - SIDEBAR_EDGE_HEIGHT,
            SIDEBAR_TEXTURE_X,
            SIDEBAR_BOTTOM_TEXTURE_Y,
            width,
            SIDEBAR_EDGE_HEIGHT,
            CONTAINER_TEXTURE_SIZE,
            CONTAINER_TEXTURE_SIZE
        );
    }

    private void suppressClientSortButtonsIfChanged() {
        if (children().size() == checkedChildCount) {
            return;
        }
        for (int index = children().size() - 1; index >= 0; index--) {
            GuiEventListener child = children().get(index);
            if (findSuperclass(child.getClass(), CLIENTSORT_TRIGGER_BUTTON_CLASS) != null) {
                removeWidget(child);
            }
        }
        checkedChildCount = children().size();
    }

    private static Class<?> findSuperclass(Class<?> type, String className) {
        Class<?> current = type;
        while (current != null) {
            if (current.getName().equals(className)) {
                return current;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static final class GlyphButton extends Button {
        private final Identifier glyph;

        private GlyphButton(int x, int y, Identifier glyph, Component narration, OnPress onPress) {
            super(x, y, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, narration, onPress, DEFAULT_NARRATION);
            this.glyph = glyph;
            setTooltip(Tooltip.create(narration));
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderDefaultSprite(graphics);
            int x = getX() + (getWidth() - HEADER_GLYPH_SIZE) / 2;
            int y = getY() + (getHeight() - HEADER_GLYPH_SIZE) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, glyph, x, y, 0.0F, 0.0F, HEADER_GLYPH_SIZE, HEADER_GLYPH_SIZE, HEADER_GLYPH_SIZE, HEADER_GLYPH_SIZE);
        }
    }
}
