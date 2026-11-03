package com.bentobox.client;

import com.bentobox.BentoBoxMod;
import com.bentobox.ChopsticksItem;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/** Preserves the imported geometry/UVs and opens each stick around its own grip. */
public final class ChopsticksRenderer extends BlockEntityWithoutLevelRenderer {
    private static ChopsticksRenderer instance;
    private ResourceManager resources;
    private Mesh mesh;
    private record Face(int texture, float[] normal, float[][] vertices) {}
    private record Part(float[] pivot, float[] tip, float direction, Face[] faces) {}
    private record Mesh(float[] displayPivot, float[] displayRotation, float openAngle, Part[] parts) {}

    private ChopsticksRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static ChopsticksRenderer instance() {
        if (instance == null) instance = new ChopsticksRenderer();
        return instance;
    }

    @Override public void onResourceManagerReload(ResourceManager manager) {
        resources = manager;
        mesh = null;
    }

    private Mesh mesh() {
        var manager = Minecraft.getInstance().getResourceManager();
        if (resources != manager) onResourceManagerReload(manager);
        if (mesh == null) {
            try (var reader = manager.openAsReader(new ResourceLocation(BentoBoxMod.MOD_ID, "bento/chopsticks.json"))) {
                mesh = new Gson().fromJson(reader, Mesh.class);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot load imported chopsticks model", e);
            }
        }
        return mesh;
    }

    private static Vector3f tip(Part part, float angle) {
        float[] p = part.pivot;
        return new Vector3f(part.tip[0]-p[0], part.tip[1]-p[1], part.tip[2]-p[2])
            .rotateZ((float)Math.toRadians(angle)).add(p[0], p[1], p[2]);
    }

    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        var mc = Minecraft.getInstance();
        Mesh model = mesh();
        ItemStack food = ChopsticksItem.heldFood(stack);
        float open = 0;
        if (!food.isEmpty()) {
            open = 1;
            if (mc.level != null && stack.hasTag() && stack.getTag().contains(ChopsticksItem.PICKED_AT)) {
                double elapsed = mc.level.getGameTime() + mc.getFrameTime() - stack.getTag().getLong(ChopsticksItem.PICKED_AT);
                if (elapsed >= 0) open = Mth.clamp((float)(elapsed / 6), 0, 1);
                open = open * open * (3 - 2 * open);
            }
        }
        pose.pushPose();
        boolean thirdPerson = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (thirdPerson || context.firstPerson()) {
            // ItemInHandLayer attaches the item origin to the player's palm. Anchor the
            // two thick grip ends there, rather than the center of the display model.
            pose.translate(0.5, 0.5, 0.5);
            float unit = (thirdPerson ? 1.0F : 1.6F) / 16;
            pose.scale(unit, unit, unit);
            float[] leftGrip = model.parts[0].pivot;
            float[] rightGrip = model.parts[1].pivot;
            pose.translate(-(leftGrip[0]+rightGrip[0])/2,
                -(leftGrip[1]+rightGrip[1])/2, -(leftGrip[2]+rightGrip[2])/2);
        } else {
            // The artist's diagonal Display group is for inventory/frame presentation;
            // applying it in hand would rotate the grips away from the player's palm.
            pose.translate(0.5, 1.0 / 16, 0.5);
            pose.scale(2.0F / 16, 2.0F / 16, 2.0F / 16);
            float[] center = model.displayPivot;
            float[] rotation = model.displayRotation;
            pose.translate(center[0], center[1], center[2]);
            pose.mulPose(Axis.ZP.rotationDegrees(rotation[2]));
            pose.mulPose(Axis.YP.rotationDegrees(rotation[1]));
            pose.mulPose(Axis.XP.rotationDegrees(rotation[0]));
            pose.translate(-center[0], -center[1], -center[2]);
        }
        for (Part part : model.parts) {
            pose.pushPose();
            float[] p = part.pivot;
            pose.translate(p[0], p[1], p[2]);
            pose.mulPose(Axis.ZP.rotationDegrees(part.direction * model.openAngle * open));
            pose.translate(-p[0], -p[1], -p[2]);
            for (Face face : part.faces) {
                var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(new ResourceLocation(
                    BentoBoxMod.MOD_ID, "textures/item/chopsticks_" + face.texture + ".png")));
                var transform = pose.last();
                for (float[] v : face.vertices) {
                    consumer.vertex(transform.pose(), v[0], v[1], v[2]).color(255,255,255,255)
                        .uv(v[3], v[4]).overlayCoords(overlay).uv2(light)
                        .normal(transform.normal(), face.normal[0], face.normal[1], face.normal[2]).endVertex();
                }
            }
            pose.popPose();
        }
        if (!food.isEmpty()) {
            // Use the same pivot math as the sticks so food stays centered as they open.
            Vector3f left = tip(model.parts[0], model.parts[0].direction * model.openAngle * open);
            Vector3f right = tip(model.parts[1], model.parts[1].direction * model.openAngle * open);
            float gap = Math.abs(left.x - right.x);
            float size = Math.min(2.6F, Math.max(0.01F, (gap - 0.4F) * 0.86F));
            Vector3f midpoint = left.add(right).mul(0.5F);
            pose.pushPose();
            pose.translate(midpoint.x, midpoint.y + 0.65F, midpoint.z);
            pose.scale(size, size, size);
            mc.getItemRenderer().renderStatic(food, ItemDisplayContext.FIXED, light, overlay, pose, buffers, mc.level, 0);
            pose.popPose();
        }
        pose.popPose();
    }
}
