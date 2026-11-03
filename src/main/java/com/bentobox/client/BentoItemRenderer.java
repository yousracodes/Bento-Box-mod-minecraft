package com.bentobox.client;

import com.bentobox.BentoBoxItem;
import com.bentobox.BentoBoxMod;
import com.bentobox.BentoBlock;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders imported Blockbench cube faces with an independently rotating lid. */
public final class BentoItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static BentoItemRenderer instance;
    private final Map<String, Mesh> meshes = new HashMap<>();
    private ResourceManager resources;
    private record Face(boolean lid, int texture, float[] normal, float[][] vertices) {}
    private record Mesh(float[] pivot, float openAngle, Face[] faces) {}

    private BentoItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static BentoItemRenderer instance() {
        if (instance == null) instance = new BentoItemRenderer();
        return instance;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        meshes.clear();
        resources = manager;
    }

    private Mesh load(String name) {
        try (var reader = Minecraft.getInstance().getResourceManager()
                .openAsReader(new ResourceLocation(BentoBoxMod.MOD_ID, "bento/" + name + ".json"))) {
            return new Gson().fromJson(reader, Mesh.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load bento model " + name, e);
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof BentoBoxItem item)) return;
        var mc = Minecraft.getInstance();
        double time = mc.level == null ? 0 : mc.level.getGameTime() + mc.getFrameTime();
        float open = BentoBoxItem.openness(stack, time);
        renderModel(item.model(), open, pose, buffers, light, overlay, 0.25);
    }

    public void renderModel(String name, float open, PoseStack pose, MultiBufferSource buffers,
                            int light, int overlay, double height) {
        var mc = Minecraft.getInstance();
        if (resources != mc.getResourceManager()) onResourceManagerReload(mc.getResourceManager());
        Mesh mesh = meshes.computeIfAbsent(name, this::load);
        pose.pushPose();
        pose.translate(0.5, height, 0.5);
        float scale = BentoBlock.renderScale(name.equals("deluxe_bento") ? 9 : name.equals("compact_bento") ? 4 : 6) / 16;
        pose.scale(scale, scale, scale);
        for (boolean lid : new boolean[]{false, true}) {
            pose.pushPose();
            if (lid) {
                float[] p = mesh.pivot;
                pose.translate(p[0], p[1], p[2]);
                pose.mulPose(Axis.XP.rotationDegrees(90 + (mesh.openAngle - 90) * open));
                pose.translate(-p[0], -p[1], -p[2]);
            }
            for (Face face : mesh.faces) {
                if (face.lid != lid) continue;
                VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(
                    new ResourceLocation(BentoBoxMod.MOD_ID, "textures/item/" + name + "_" + face.texture + ".png")));
                var transform = pose.last();
                for (float[] v : face.vertices) {
                    consumer.vertex(transform.pose(), v[0], v[1], v[2]).color(255,255,255,255)
                        .uv(v[3],v[4]).overlayCoords(overlay).uv2(light)
                        .normal(transform.normal(), face.normal[0],face.normal[1],face.normal[2]).endVertex();
                }
            }
            pose.popPose();
        }
        pose.popPose();
    }
}
