package com.bentobox.client;

import com.bentobox.BentoBoxMod;
import com.bentobox.ModContent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BentoBoxMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BentoClientEvents {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModContent.BENTO.get(), BentoBlockRenderer::new);
    }
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModContent.MENU.get(), BentoScreen::new));
    }
    @SubscribeEvent
    public static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(BentoItemRenderer.instance());
        event.registerReloadListener(BentoFoodDisplays.INSTANCE);
    }
}
