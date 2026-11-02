package com.bentobox;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BentoBoxMod.MOD_ID)
public final class BentoInteractions {
    // Sneaking normally bypasses block interaction when holding an item.
    @SubscribeEvent
    public static void shiftClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity().isShiftKeyDown()
                && event.getLevel().getBlockEntity(event.getPos()) instanceof BentoBlockEntity box) {
            if (!event.getLevel().isClientSide && event.getEntity().mayBuild()) box.toggleLid();
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        }
    }
}
