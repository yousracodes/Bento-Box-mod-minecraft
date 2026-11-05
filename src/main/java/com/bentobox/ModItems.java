package com.bentobox;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BentoBoxMod.MOD_ID);
    public static final RegistryObject<Item> CHOPSTICKS = ITEMS.register("wooden_chopsticks", ChopsticksItem::new);
    public static final RegistryObject<Item> COMPACT = ITEMS.register("compact_bento", () -> new BentoBoxItem("compact_bento", ModContent.COMPACT.get()));
    public static final RegistryObject<Item> CLASSIC = ITEMS.register("classic_bento", () -> new BentoBoxItem("classic_bento", ModContent.CLASSIC.get()));
    public static final RegistryObject<Item> DELUXE = ITEMS.register("deluxe_bento", () -> new BentoBoxItem("deluxe_bento", ModContent.DELUXE.get()));
    public static final RegistryObject<Item> BUNNY_PICK = ITEMS.register("bunny_food_pick", FoodPickItem::new);
    public static final RegistryObject<Item> BEAR_PICK = ITEMS.register("bear_food_pick", FoodPickItem::new);
    public static final RegistryObject<Item> BOW_PICK = ITEMS.register("bow_food_pick", FoodPickItem::new);
    public static final RegistryObject<Item> STAR_PICK = ITEMS.register("star_food_pick", FoodPickItem::new);
    public static final RegistryObject<Item> CLOVER_PICK = ITEMS.register("clover_food_pick", FoodPickItem::new);
    public static final RegistryObject<Item> HEART_PICK = ITEMS.register("heart_food_pick", FoodPickItem::new);
    public static final RegistryObject<Item> BLOSSOM_PICK = ITEMS.register("blossom_food_pick", FoodPickItem::new);
    public static final java.util.List<RegistryObject<Item>> FOOD_PICKS = java.util.List.of(
        BUNNY_PICK, BEAR_PICK, BOW_PICK, STAR_PICK, CLOVER_PICK, HEART_PICK, BLOSSOM_PICK);

    public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(CHOPSTICKS);
            event.accept(COMPACT);
            event.accept(CLASSIC);
            event.accept(DELUXE);
            FOOD_PICKS.forEach(event::accept);
        }
    }
}
