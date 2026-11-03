package com.bentobox.client;

import com.bentobox.BentoBlock;
import com.bentobox.BentoBlockEntity;
import com.bentobox.BentoCompartments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;

/** Draws the actual food item icons as shallow piles inside the model compartments. */
public final class BentoFoodRenderer {
    private BentoFoodRenderer() {}

    public static void render(BentoBlockEntity box, float open, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        // Keep food below the lid throughout closing; reveal it once the lid clears the tray.
        if (open < 0.12F) return;
        boolean deluxe = box.contents.getSlots() == 9;
        float unit = BentoBlock.renderScale(box.contents.getSlots()) / 16;
        var renderer = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.scale(unit, unit, unit);
        for (int slot = 0; slot < box.contents.getSlots(); slot++) {
            var stack = box.contents.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            int maximum = Math.max(1, stack.getMaxStackSize());
            int layers = maximum == 1 ? 1 : 1 + (int)Math.ceil(
                Math.min(stack.getCount() - 1, maximum - 1) * 5.0 / (maximum - 1));
            float x = BentoCompartments.x(box.contents.getSlots(), slot);
            float z = BentoCompartments.z(box.contents.getSlots(), slot);
            float size = deluxe ? 2.8F : 3.9F;
            var display = BentoFoodDisplays.INSTANCE.find(stack);
            if (display.isPresent()) {
                var block = display.get();
                float fit = Math.min(size / Math.max(0.001F, block.maxX()-block.minX()),
                    size / Math.max(0.001F, block.maxZ()-block.minZ()));
                fit = Math.min(fit, (deluxe ? 1.3F : 3.2F) / Math.max(0.001F, block.maxY()-block.minY()));
                pose.pushPose();
                pose.translate(x, deluxe ? 3.65F : 1.65F, z);
                pose.scale(fit, fit, fit);
                pose.translate(-(block.minX()+block.maxX())/2, -block.minY(), -(block.minZ()+block.maxZ())/2);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.state(), pose, buffers, light, overlay);
                pose.popPose();
                continue;
            }
            for (int layer = 0; layer < layers; layer++) {
                pose.pushPose();
                pose.translate(x, (deluxe ? 3.65F : 2.05F) + layer * (deluxe ? 0.22F : 0.48F), z);
                pose.mulPose(Axis.YP.rotationDegrees(layer % 2 == 0 ? -4 : 4));
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                // Flatten the item's GUI appearance into a food texture layer. Minecraft
                // supplies its own model overrides, tint, glint and resource-pack textures.
                pose.scale(size, size, 0.035F);
                renderer.renderStatic(stack, ItemDisplayContext.GUI, light, overlay,
                    pose, buffers, box.getLevel(), slot);
                pose.popPose();
            }
        }
        pose.popPose();
    }
}
