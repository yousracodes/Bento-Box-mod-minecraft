package com.bentobox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared model-space food positions for rendering and picking compartments. */
public final class BentoCompartments {
    private BentoCompartments() {}

    public static int columns(int count) { return count == 4 ? 2 : 3; }
    public static float x(int count, int slot) {
        return count == 4 ? (slot % 2 == 0 ? -3.25F : 3.25F)
            : (slot % 3 - 1) * (count == 9 ? 4.5F : 6.25F);
    }
    public static float z(int count, int slot) {
        return count == 9 ? (slot / 3 - 1) * 4F
            : (slot / columns(count) == 0 ? -3.25F : 3.25F);
    }

    public static int slotAt(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (!(state.getBlock() instanceof BentoBlock block) || !hit.getBlockPos().equals(pos)
                || hit.getDirection() != Direction.UP) return -1;
        double dx = hit.getLocation().x - pos.getX() - 0.5;
        double dz = hit.getLocation().z - pos.getZ() - 0.5;
        double yaw = Math.toRadians(state.getValue(BentoBlock.FACING).toYRot());
        double unit = BentoBlock.renderScale(block.compartments()) / 16;
        // Invert the model's -yaw rotation around the center of the block.
        double x = (Math.cos(yaw) * dx + Math.sin(yaw) * dz) / unit;
        double z = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / unit;
        int count = block.compartments();
        for (int slot = 0; slot < count; slot++) {
            if (Math.abs(x - x(count, slot)) < (count == 9 ? 1.55 : 2.6)
                    && Math.abs(z - z(count, slot)) < (count == 9 ? 1.3 : 2.6)) return slot;
        }
        return -1; // Rim, latch, and dividers are not servings.
    }
}
