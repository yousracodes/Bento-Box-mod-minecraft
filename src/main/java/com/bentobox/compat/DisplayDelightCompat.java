package com.bentobox.compat;

import com.bentobox.BentoBoxMod;
import java.lang.reflect.Field;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModList;

/** Optional bridge to Display Delight 1.4.x's own initialized placement associations. */
public final class DisplayDelightCompat {
    private static boolean initialized;
    private static Field[] associations = new Field[0];

    private DisplayDelightCompat() {}

    public static BlockState singleServingState(BlockState state) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null && id.getNamespace().equals("displaydelight")
                && state.getBlock().getStateDefinition().getProperty("stacks") instanceof IntegerProperty stacks
                && stacks.getPossibleValues().contains(1)) {
            return state.setValue(stacks, 1);
        }
        return state;
    }

    @Nullable
    public static Block findBlock(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) return null;
        if (!initialized) initialize();
        try {
            // Read live maps: do not snapshot them before Display Delight populates them.
            for (Field field : associations) {
                if (field.get(null) instanceof Map<?, ?> map
                        && map.get(stack.getItem()) instanceof Block block
                        && !block.defaultBlockState().isAir()) return block;
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            disable(exception);
        }
        return null;
    }

    private static void initialize() {
        initialized = true;
        if (!ModList.get().isLoaded("displaydelight")) return;
        try {
            Class<?> type = Class.forName("com.jkvin114.displaydelight.init.BlockAssociations");
            associations = new Field[] {
                type.getField("blockMap"),
                type.getField("smallplateBlockMap"),
                type.getField("plateBlockMap")
            };
            BentoBoxMod.LOGGER.info("Enabled automatic Display Delight food displays");
        } catch (ReflectiveOperationException | LinkageError exception) {
            disable(exception);
        }
    }

    private static void disable(Throwable exception) {
        associations = new Field[0];
        BentoBoxMod.LOGGER.warn("Display Delight association API unavailable; keeping standard bento food displays", exception);
    }
}
