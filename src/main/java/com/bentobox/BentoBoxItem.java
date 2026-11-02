package com.bentobox;

import com.bentobox.client.BentoItemRenderer;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class BentoBoxItem extends Item {
    public static final String OPEN = "BentoOpen";
    public static final String CHANGED = "BentoLidChanged";
    public static final String FROM = "BentoLidFrom";
    public static final float DURATION = 10.0F;
    private final String model;

    public BentoBoxItem(String model) {
        super(new Item.Properties().stacksTo(1));
        this.model = model;
    }

    public String model() { return model; }

    public static float openness(ItemStack stack, double time) {
        var tag = stack.getTag();
        if (tag == null) return 0;
        float target = tag.getBoolean(OPEN) ? 1 : 0;
        if (!tag.contains(CHANGED)) return target;
        float t = net.minecraft.util.Mth.clamp((float)(time - tag.getLong(CHANGED)) / DURATION, 0, 1);
        t = t * t * (3 - 2 * t);
        return net.minecraft.util.Mth.lerp(t, tag.getFloat(FROM), target);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            float from = openness(stack, level.getGameTime());
            var tag = stack.getOrCreateTag();
            tag.putFloat(FROM, from);
            tag.putLong(CHANGED, level.getGameTime());
            tag.putBoolean(OPEN, !tag.getBoolean(OPEN));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return BentoItemRenderer.instance();
            }
        });
    }
}
