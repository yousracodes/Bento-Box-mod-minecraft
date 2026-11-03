package com.bentobox;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;

public final class ModContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BentoBoxMod.MOD_ID);
    public static final RegistryObject<Block> COMPACT = BLOCKS.register("compact_bento", () -> new BentoBlock(4));
    public static final RegistryObject<Block> CLASSIC = BLOCKS.register("classic_bento", () -> new BentoBlock(6));
    public static final RegistryObject<Block> DELUXE = BLOCKS.register("deluxe_bento", () -> new BentoBlock(9));
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BentoBoxMod.MOD_ID);
    public static final RegistryObject<BlockEntityType<BentoBlockEntity>> BENTO = BLOCK_ENTITIES.register("bento", () ->
        BlockEntityType.Builder.of(BentoBlockEntity::new, COMPACT.get(), CLASSIC.get(), DELUXE.get()).build(null));
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BentoBoxMod.MOD_ID);
    public static final RegistryObject<MenuType<BentoMenu>> MENU = MENUS.register("bento", () -> IForgeMenuType.create(BentoMenu::new));
}
