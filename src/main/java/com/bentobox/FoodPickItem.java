package com.bentobox;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** A reusable decoration, stored separately from the food in its compartment. */
public final class FoodPickItem extends Item {
    public FoodPickItem() { super(new Properties()); }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bentobox.food_pick.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
