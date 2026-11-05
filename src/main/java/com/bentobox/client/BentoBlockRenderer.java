package com.bentobox.client;

import com.bentobox.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;

public final class BentoBlockRenderer implements BlockEntityRenderer<BentoBlockEntity> {
    public BentoBlockRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(BentoBlockEntity box, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        String name = ((BentoBlock)box.getBlockState().getBlock()).model();
        float open = box.openness(box.getLevel().getGameTime() + partialTick);
        pose.pushPose();
        pose.translate(0.5,0,0.5);
        // Imported models have their latch/front at +Z (south); FACING points toward the opener.
        pose.mulPose(Axis.YP.rotationDegrees(-box.getBlockState().getValue(BentoBlock.FACING).toYRot()));
        pose.translate(-0.5,0,-0.5);
        BentoItemRenderer.instance().renderModel(name, open, pose, buffers, light, overlay, 0);
        BentoFoodRenderer.render(box, open, pose, buffers, light, overlay);
        BentoDecorationRenderer.render(box, open, pose, buffers, light, overlay);
        pose.popPose();
    }
}
