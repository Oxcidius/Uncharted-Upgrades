package uk.co.newcollegeworcester.uo.upgradekits;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;

final class UoMenus {
    private static MenuType<UoChestMenu> chest;

    private UoMenus() {
    }

    static void register() {
        chest = Registry.register(
                BuiltInRegistries.MENU,
                UoRegistries.id("uo_chest"),
                new ExtendedMenuType<>(UoChestMenu::new, ByteBufCodecs.VAR_INT)
        );
    }

    static MenuType<UoChestMenu> chest() {
        return chest;
    }
}
