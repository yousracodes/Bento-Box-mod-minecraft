package com.bentobox;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BentoBoxMod.MOD_ID);
    public static final RegistryObject<Item> CLASSIC = ITEMS.register("classic_bento", () -> new BentoBoxItem("classic_bento", ModContent.CLASSIC.get()));
    public static final RegistryObject<Item> DELUXE = ITEMS.register("deluxe_bento", () -> new BentoBoxItem("deluxe_bento", ModContent.DELUXE.get()));

    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(CLASSIC);
            event.accept(DELUXE);
        }
    }
}
