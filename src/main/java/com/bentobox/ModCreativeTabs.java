package com.bentobox;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BentoBoxMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BENTO_BOX = TABS.register("bento_box", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bentobox"))
            .icon(() -> new ItemStack(ModItems.CLASSIC.get()))
            .withTabsBefore(CreativeModeTabs.FOOD_AND_DRINKS)
            .displayItems((parameters, output) -> {
                output.accept(ModItems.CLASSIC.get());
                output.accept(ModItems.DELUXE.get());
            })
            .build());
}
