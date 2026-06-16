package uk.co.newcollegeworcester.uo.upgradekits.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.newcollegeworcester.uo.upgradekits.TieredBundleItem;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin {
    private static final float UO_COUNTER_SCALE = 0.5F;

    @Inject(
            method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL")
    )
    private void uncharted_upgrades$renderBundleCount(
            Font font,
            ItemStack stack,
            int x,
            int y,
            String countLabel,
            CallbackInfo ci
    ) {
        if (!(stack.getItem() instanceof TieredBundleItem)) {
            return;
        }

        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (contents.isEmpty()) {
            return;
        }

        String text = Integer.toString(contents.weight().getOrThrow().multiplyBy(Fraction.getFraction(64, 1)).intValue());
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(UO_COUNTER_SCALE, UO_COUNTER_SCALE);

        int scaledRight = Math.round((x + 16) / UO_COUNTER_SCALE);
        int scaledTop = Math.round((y + 1) / UO_COUNTER_SCALE);
        graphics.text(font, text, scaledRight - font.width(text), scaledTop, 0xFFFFFFFF, true);

        pose.popMatrix();
    }
}
