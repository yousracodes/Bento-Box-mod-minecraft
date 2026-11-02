package com.bentobox;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Entry point for Bento Box. Register future mod content here. */
@Mod(BentoBoxMod.MOD_ID)
public final class BentoBoxMod {
    public static final String MOD_ID = "bentobox";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BentoBoxMod() {
        var bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModContent.BLOCKS.register(bus);
        ModContent.BLOCK_ENTITIES.register(bus);
        ModContent.MENUS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        bus.addListener(ModItems::creativeTab);
        LOGGER.info("Bento Box initialized");
    }
}
