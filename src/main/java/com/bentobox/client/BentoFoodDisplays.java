package com.bentobox.client;

import com.bentobox.BentoBoxMod;
import com.bentobox.compat.DisplayDelightCompat;
import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

/** Client resource mappings override automatic BlockItem detection. */
public final class BentoFoodDisplays extends SimpleJsonResourceReloadListener {
    public static final BentoFoodDisplays INSTANCE = new BentoFoodDisplays();
    private final Map<ResourceLocation, BlockState> mappings = new HashMap<>();
    private final Map<BlockState, Optional<Display>> displays = new HashMap<>();
    public record Display(BlockState state, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}

    private BentoFoodDisplays() { super(new Gson(), "bento_food_displays"); }

    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        mappings.clear();
        displays.clear();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                ResourceLocation item = new ResourceLocation(json.get("item").getAsString());
                ResourceLocation block = new ResourceLocation(json.get("block").getAsString());
                // An absent optional food mod should not break resource loading.
                if (!ForgeRegistries.ITEMS.containsKey(item) || !ForgeRegistries.BLOCKS.containsKey(block)) return;
                BlockState state = ForgeRegistries.BLOCKS.getValue(block).defaultBlockState();
                if (json.has("properties")) {
                    for (var property : json.getAsJsonObject("properties").entrySet()) {
                        state = setProperty(state, property.getKey(), property.getValue().getAsString());
                    }
                }
                mappings.put(item, state);
            } catch (RuntimeException exception) {
                BentoBoxMod.LOGGER.warn("Ignoring invalid bento food display {}", entry.getKey(), exception);
            }
        });
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, String name, String value) {
        @SuppressWarnings("unchecked")
        Property<T> property = (Property<T>) state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) throw new IllegalArgumentException("Unknown block property " + name);
        return state.setValue(property, property.getValue(value).orElseThrow(() -> new IllegalArgumentException("Invalid " + name + ": " + value)));
    }

    public Optional<Display> find(ItemStack stack) {
        if (stack.getCount() != 1) return Optional.empty();
        BlockState state = mappings.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        if (state == null) {
            var block = DisplayDelightCompat.findBlock(stack);
            if (block != null) state = block.defaultBlockState();
        }
        if (state == null && stack.getItem() instanceof BlockItem item) state = item.getBlock().defaultBlockState();
        if (state == null || state.getRenderShape() != RenderShape.MODEL) return Optional.empty();
        // Display Delight defaults several plated blocks to six servings, even
        // though this branch is only used when the compartment holds one item.
        state = DisplayDelightCompat.singleServingState(state);
        return displays.computeIfAbsent(state, this::measure);
    }

    private Optional<Display> measure(BlockState state) {
        try {
            var mc = Minecraft.getInstance();
            var model = mc.getBlockRenderer().getBlockModel(state);
            if (model == mc.getModelManager().getMissingModel() || model.isCustomRenderer()) return Optional.empty();
            float[] min = {Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY};
            float[] max = {Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
            // Include culled and unculled faces when fitting the baked geometry to a compartment.
            for (int side = 0; side < 7; side++) {
                Direction direction = side == 6 ? null : Direction.values()[side];
                for (var quad : model.getQuads(state, direction, RandomSource.create(42))) {
                    int[] vertices = quad.getVertices();
                    int stride = vertices.length / 4;
                    for (int vertex = 0; vertex < 4; vertex++) for (int axis = 0; axis < 3; axis++) {
                        float value = Float.intBitsToFloat(vertices[vertex * stride + axis]);
                        if (!Float.isFinite(value)) return Optional.empty();
                        min[axis] = Math.min(min[axis], value);
                        max[axis] = Math.max(max[axis], value);
                    }
                }
            }
            if (!Float.isFinite(min[0])) return Optional.empty();
            return Optional.of(new Display(state,min[0],min[1],min[2],max[0],max[1],max[2]));
        } catch (RuntimeException exception) {
            BentoBoxMod.LOGGER.warn("Using item texture for unsupported food block {}", state, exception);
            return Optional.empty();
        }
    }
}
