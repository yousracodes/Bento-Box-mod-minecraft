package com.bentobox;

import com.bentobox.client.ChopsticksRenderer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** One persistent serving can be picked up, carried, and eaten separately. */
public final class ChopsticksItem extends Item {
    public static final String PICKED_AT = "BentoPickedAt";
    private static final String FOOD = "BentoHeldFood";
    public ChopsticksItem() { super(new Properties().stacksTo(1)); }

    public static ItemStack heldFood(ItemStack sticks) {
        if (!sticks.hasTag() || !sticks.getTag().contains(FOOD)) return ItemStack.EMPTY;
        ItemStack food = ItemStack.of(sticks.getTag().getCompound(FOOD));
        return BentoBlockEntity.validFood(food) ? food.copyWithCount(1) : ItemStack.EMPTY;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof BentoBlockEntity box)
            return interactWithBento(player, context.getHand(), box,
                new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()));
        return heldFood(context.getItemInHand()).isEmpty() ? InteractionResult.PASS
            : use(context.getLevel(), player, context.getHand()).getResult();
    }

    public InteractionResult interactWithBento(Player player, InteractionHand hand, BentoBlockEntity box, BlockHitResult hit) {
        ItemStack sticks = player.getItemInHand(hand);
        if (sticks.getItem() != this || player.isSpectator() || player.isUsingItem()) return InteractionResult.FAIL;
        int slot = BentoCompartments.slotAt(box.getBlockState(), box.getBlockPos(), hit);
        Level level = player.level();
        if (slot < 0 || box.isRemoved() || box.getLevel() != level || !level.hasChunkAt(box.getBlockPos())
                || level.getBlockEntity(box.getBlockPos()) != box || !box.isLidOpen()
                || box.openness(level.getGameTime()) < 0.95F) return InteractionResult.FAIL;
        // Validate the current click ray before transferring ownership to the chopsticks.
        var eye = player.getEyePosition();
        var ray = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(player.getBlockReach())),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (ray.getType() != HitResult.Type.BLOCK
                || BentoCompartments.slotAt(box.getBlockState(), box.getBlockPos(), ray) != slot) return InteractionResult.FAIL;
        ItemStack loaded = heldFood(sticks);
        if (!loaded.isEmpty()) {
            if (!box.contents.insertItem(slot, loaded, true).isEmpty()) {
                message(player, "item.bentobox.wooden_chopsticks.no_room");
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide && box.contents.insertItem(slot, loaded, false).isEmpty()) {
                clearFood(sticks);
                player.getInventory().setChanged();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack food = box.contents.getStackInSlot(slot);
        if (!BentoBlockEntity.validFood(food)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            ItemStack serving = player.getAbilities().instabuild ? food.copyWithCount(1)
                : box.contents.extractItem(slot, 1, false);
            sticks.getOrCreateTag().put(FOOD, serving.save(new net.minecraft.nbt.CompoundTag()));
            sticks.getOrCreateTag().putLong(PICKED_AT, level.getGameTime());
            player.getInventory().setChanged();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean canEat(Player player, ItemStack food) {
        var properties = food.getFoodProperties(player);
        return !food.isEmpty() && properties != null && player.canEat(properties.canAlwaysEat());
    }

    private static void message(Player player, String key) {
        if (!player.level().isClientSide) player.displayClientMessage(Component.translatable(key), true);
    }

    private static void clearFood(ItemStack sticks) {
        sticks.removeTagKey(FOOD);
        sticks.removeTagKey(PICKED_AT);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sticks = player.getItemInHand(hand);
        if (player.isSpectator()) return InteractionResultHolder.fail(sticks);
        ItemStack food = heldFood(sticks);
        if (food.isEmpty()) return InteractionResultHolder.pass(sticks);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                clearFood(sticks);
                if (!player.getInventory().add(food)) player.drop(food, false);
                player.getInventory().setChanged();
                message(player, "item.bentobox.wooden_chopsticks.unloaded");
            }
            return InteractionResultHolder.sidedSuccess(sticks, level.isClientSide);
        }
        if (!canEat(player, food)) {
            message(player, "item.bentobox.wooden_chopsticks.full");
            return InteractionResultHolder.fail(sticks);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(sticks);
    }

    @Override public int getUseDuration(ItemStack stack) {
        ItemStack food = heldFood(stack);
        return food.isEmpty() ? 0 : Math.max(1, food.getUseDuration());
    }

    // Mouth animation without vanilla EAT generating wooden item crumbs.
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public SoundEvent getDrinkingSound() { return SoundEvents.GENERIC_EAT; }

    @Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide || !(user instanceof Player player)) return;
        ItemStack food = heldFood(stack);
        if (!canEat(player, food)) {
            player.stopUsingItem();
            return;
        }
        if (remaining % 4 == 0 && getUseDuration(stack) - remaining >= 7) {
            var mouth = player.getEyePosition().add(player.getLookAngle().scale(0.25)).add(0, -0.15, 0);
            ((ServerLevel)level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food),
                mouth.x, mouth.y, mouth.z, 3, 0.06, 0.04, 0.06, 0.01);
        }
    }

    @Override public ItemStack finishUsingItem(ItemStack sticks, Level level, LivingEntity user) {
        if (!level.isClientSide && user instanceof Player player) {
            ItemStack serving = heldFood(sticks);
            if (canEat(player, serving)) {
                // Clear first so callbacks cannot consume this same serving twice.
                clearFood(sticks);
                ItemStack remainder = serving.finishUsingItem(level, player);
                if (!player.getAbilities().instabuild && !remainder.isEmpty() && !player.getInventory().add(remainder))
                    player.drop(remainder, false);
                player.getInventory().setChanged();
            }
        }
        return sticks;
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        ItemStack food = heldFood(stack);
        tooltip.add(Component.translatable(food.isEmpty() ? "item.bentobox.wooden_chopsticks.tooltip"
            : "item.bentobox.wooden_chopsticks.loaded_tooltip").withStyle(ChatFormatting.GRAY));
        if (!food.isEmpty()) tooltip.add(Component.translatable("item.bentobox.wooden_chopsticks.holding",
            food.getHoverName()).withStyle(ChatFormatting.GOLD));
        if (!food.isEmpty()) tooltip.add(Component.translatable("item.bentobox.wooden_chopsticks.remove_tooltip")
            .withStyle(ChatFormatting.GRAY));
    }

    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return ChopsticksRenderer.instance(); }
        });
    }
}
