package uk.co.newcollegeworcester.uo.upgradekits;

import net.minecraft.world.item.DyeColor;

public enum UoShulkerVariant {
    UNDYED(null, ""),
    WHITE(DyeColor.WHITE, "white"),
    ORANGE(DyeColor.ORANGE, "orange"),
    MAGENTA(DyeColor.MAGENTA, "magenta"),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE, "light_blue"),
    YELLOW(DyeColor.YELLOW, "yellow"),
    LIME(DyeColor.LIME, "lime"),
    PINK(DyeColor.PINK, "pink"),
    GRAY(DyeColor.GRAY, "gray"),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY, "light_gray"),
    CYAN(DyeColor.CYAN, "cyan"),
    PURPLE(DyeColor.PURPLE, "purple"),
    BLUE(DyeColor.BLUE, "blue"),
    BROWN(DyeColor.BROWN, "brown"),
    GREEN(DyeColor.GREEN, "green"),
    RED(DyeColor.RED, "red"),
    BLACK(DyeColor.BLACK, "black");

    public final DyeColor color;
    public final String id;

    UoShulkerVariant(DyeColor color, String id) {
        this.color = color;
        this.id = id;
    }

    public String blockId(UpgradeTier tier) {
        return id.isEmpty() ? tier.id + "_shulker_box" : id + "_" + tier.id + "_shulker_box";
    }

    public String textureId(UpgradeTier tier) {
        return tier.id + "/" + (id.isEmpty() ? "shulker" : "shulker_" + id);
    }

    public String vanillaBlockId() {
        return id.isEmpty() ? "minecraft:shulker_box" : "minecraft:" + id + "_shulker_box";
    }
}
