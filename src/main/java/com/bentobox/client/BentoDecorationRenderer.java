package com.bentobox.client;

import com.bentobox.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;

/** Reusable picks stay in their compartments when food is removed or eaten. */
public final class BentoDecorationRenderer {
    private BentoDecorationRenderer() {}

    public static void render(BentoBlockEntity box, float open, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        if (!box.isLidOpen() || open < 0.95F) return;
        int count = box.contents.getSlots();
        boolean deluxe = count == 9;
        float unit = BentoBlock.renderScale(count) / 16;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.scale(unit, unit, unit);
        for (int slot = 0; slot < count; slot++) {
            var pick = box.decorations.getStackInSlot(slot);
            if (pick.isEmpty() || box.contents.getStackInSlot(slot).isEmpty()) continue;
            float originalSize = deluxe ? 1.7F : 2.7F;
            float size = originalSize * 1.25F;
            // Lift the enlarged sprite so its stem stays at the same depth in the food.
            float lift = (size-originalSize) * 0.5F;
            pose.pushPose();
            pose.translate(BentoCompartments.x(count, slot)+(deluxe ? 0.45F : 0.85F),
                (deluxe ? 4.55F : 5.0F)+lift, BentoCompartments.z(count, slot)+(deluxe ? 0.3F : 0.55F));
            pose.mulPose(Axis.YP.rotationDegrees(slot % 2 == 0 ? -12 : 12));
            pose.mulPose(Axis.ZP.rotationDegrees(-10));
            pose.scale(size, size, size);
            Minecraft.getInstance().getItemRenderer().renderStatic(pick, ItemDisplayContext.FIXED,
                light, overlay, pose, buffers, box.getLevel(), slot);
            pose.popPose();
        }
        pose.popPose();
    }
}
