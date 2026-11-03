package com.bentobox;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder(BentoBoxMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChopsticksGameTests {
    private static final class EatingPlayer extends net.minecraftforge.common.util.FakePlayer {
        EatingPlayer(net.minecraft.server.level.ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "ChopsticksTest"));
        }
        void tickEating() { if (isUsingItem()) updateUsingItem(getUseItem()); }
    }

    private static ChopsticksItem sticks() { return (ChopsticksItem)ModItems.CHOPSTICKS.get(); }

    private static BlockHitResult hit(BlockState state, BlockPos pos, int slot) {
        int count = ((BentoBlock)state.getBlock()).compartments();
        double unit = BentoBlock.renderScale(count) / 16;
        double yaw = Math.toRadians(state.getValue(BentoBlock.FACING).toYRot());
        double x = BentoCompartments.x(count, slot) * unit;
        double z = BentoCompartments.z(count, slot) * unit;
        return new BlockHitResult(new Vec3(pos.getX()+0.5+Math.cos(yaw)*x-Math.sin(yaw)*z,
            pos.getY()+6.45*unit, pos.getZ()+0.5+Math.sin(yaw)*x+Math.cos(yaw)*z), Direction.UP, pos, false);
    }

    private static void aim(Player player, Vec3 point) {
        Vec3 delta = point.subtract(player.getEyePosition());
        player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
    }

    private static Player player(GameTestHelper h, BentoBlockEntity box) {
        var player = new EatingPlayer(h.getLevel());
        player.setPos(box.getBlockPos().getX()+0.5, box.getBlockPos().getY(), box.getBlockPos().getZ()+1.5);
        player.getAbilities().instabuild = false;
        player.getInventory().clearContent();
        player.getFoodData().setFoodLevel(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(sticks()));
        aim(player, hit(box.getBlockState(), box.getBlockPos(), 0).getLocation());
        return player;
    }

    private static BentoBlockEntity box(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlock(pos, ModContent.COMPACT.get().defaultBlockState(), 3);
        var box = (BentoBlockEntity)h.getLevel().getBlockEntity(pos);
        ItemStack stack = new ItemStack(ModItems.COMPACT.get());
        stack.getOrCreateTag().putBoolean(BentoBoxItem.OPEN, true);
        box.setBox(stack);
        return box;
    }

    private static boolean pickup(Player player, BentoBlockEntity box) {
        return sticks().interactWithBento(player, InteractionHand.MAIN_HAND, box,
            hit(box.getBlockState(), box.getBlockPos(), 0)).consumesAction();
    }

    private static void finish(Player player) {
        ItemStack held = player.getMainHandItem();
        sticks().finishUsingItem(held, player.level(), player);
        player.stopUsingItem();
    }

    @GameTest(template = "empty")
    public static void pickingAllSizesAndFacings(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1,1,1));
        for (var registered : new net.minecraft.world.level.block.Block[]{ModContent.COMPACT.get(), ModContent.CLASSIC.get(), ModContent.DELUXE.get()}) {
            var block = (BentoBlock)registered;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var state = block.defaultBlockState().setValue(BentoBlock.FACING, facing);
                for (int slot = 0; slot < block.compartments(); slot++) {
                    var hit = hit(state, pos, slot);
                    h.assertTrue(BentoCompartments.slotAt(state, pos, hit) == slot, "Wrong rotated compartment");
                    h.assertTrue(BentoCompartments.slotAt(state, pos,
                        new BlockHitResult(hit.getLocation(), Direction.NORTH, pos, false)) == -1, "Side selected food");
                }
                h.assertTrue(BentoCompartments.slotAt(state, pos,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)) == (block.compartments()==9 ? 4 : -1),
                    "Divider/center picking mismatch");
            }
        }
        h.succeed();
    }

    private static boolean begin(Player player) {
        return sticks().use(player.level(), player, InteractionHand.MAIN_HAND).getResult().consumesAction();
    }

    @GameTest(template = "empty")
    public static void pickupThenEat(GameTestHelper h) {
        var box = box(h);
        var player = player(h, box);
        ItemStack apples = new ItemStack(Items.APPLE, 3);
        apples.getOrCreateTag().putString("Lunch", "preserve me");
        box.contents.setStackInSlot(0, apples);
        h.assertTrue(pickup(player, box), "Could not pick up serving");
        h.assertTrue(!player.isUsingItem(), "Picking up immediately started eating");
        h.assertTrue(player.getFoodData().getFoodLevel()==0, "Picking up fed the player");
        h.assertTrue(box.contents.getStackInSlot(0).getCount()==2, "Did not transfer exactly one serving");
        var held = ChopsticksItem.heldFood(player.getMainHandItem());
        h.assertTrue(held.getCount()==1 && held.getTag().getString("Lunch").equals("preserve me"), "Lost food or NBT");
        var saved = player.getMainHandItem().save(new net.minecraft.nbt.CompoundTag());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.of(saved));
        h.assertTrue(ItemStack.matches(held, ChopsticksItem.heldFood(player.getMainHandItem())), "Save/load lost carried food");
        // Loaded chopsticks can eat anywhere, independent of the original bento.
        h.getLevel().setBlock(box.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
        player.setPos(player.getX()+12, player.getY(), player.getZ());
        player.setYRot(player.getYRot()+180);
        h.assertTrue(begin(player), "Could not eat carried serving away from box");
        h.assertTrue(player.getMainHandItem().getUseDuration()==32, "Wrong eating duration");
        finish(player);
        h.assertTrue(player.getFoodData().getFoodLevel()==4, "Missing nutrition");
        h.assertTrue(ChopsticksItem.heldFood(player.getMainHandItem()).isEmpty(), "Food remained after eating");
        h.assertTrue(player.getMainHandItem().is(ModItems.CHOPSTICKS.get()), "Lost chopsticks");
        sticks().finishUsingItem(player.getMainHandItem(), h.getLevel(), player);
        h.assertTrue(player.getFoodData().getFoodLevel()==4, "Consumed twice");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foodEffectsAndContainers(GameTestHelper h) {
        var box = box(h);
        var player = player(h, box);
        box.contents.setStackInSlot(0, new ItemStack(Items.MUSHROOM_STEW));
        h.assertTrue(pickup(player, box) && begin(player), "Could not eat stew");
        finish(player);
        h.assertTrue(box.contents.getStackInSlot(0).isEmpty(), "Stew remained in box");
        h.assertTrue(player.getInventory().countItem(Items.BOWL)==1, "Lost bowl");
        player.getFoodData().setFoodLevel(20);
        box.contents.setStackInSlot(0, new ItemStack(Items.GOLDEN_APPLE));
        h.assertTrue(pickup(player, box) && begin(player), "Always-edible food rejected when full");
        finish(player);
        h.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "Lost food effect");
        player.getFoodData().setFoodLevel(0);
        box.contents.setStackInSlot(0, new ItemStack(Items.DRIED_KELP));
        h.assertTrue(pickup(player, box) && begin(player) && player.getMainHandItem().getUseDuration()==16,
            "Fast food duration ignored");
        player.releaseUsingItem();
        h.assertTrue(ChopsticksItem.heldFood(player.getMainHandItem()).is(Items.DRIED_KELP), "Cancel lost serving");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cancellationAndSharing(GameTestHelper h) {
        var box = box(h);
        var player = player(h, box);
        var second = player(h, box);
        box.contents.setStackInSlot(0, new ItemStack(Items.APPLE));
        player.getFoodData().setFoodLevel(20);
        h.assertTrue(pickup(player, box), "Full player could not pick up food for later");
        h.assertTrue(!begin(player), "Full player ate ordinary food");
        h.assertTrue(!pickup(second, box), "Two players picked up the same last serving");
        h.assertTrue(ChopsticksItem.heldFood(second.getMainHandItem()).isEmpty(), "Duplicated carried food");
        player.getFoodData().setFoodLevel(0);
        h.assertTrue(begin(player), "Could not begin eating loaded food");
        player.releaseUsingItem();
        h.assertTrue(ChopsticksItem.heldFood(player.getMainHandItem()).is(Items.APPLE), "Release lost serving");
        h.assertTrue(player.getFoodData().getFoodLevel()==0, "Release fed player");
        // A second pickup must never overwrite the serving already on the sticks.
        box.contents.setStackInSlot(0, new ItemStack(Items.CARROT, 2));
        h.assertTrue(!pickup(player, box), "Incompatible compartment accepted carried food");
        h.assertTrue(box.contents.getStackInSlot(0).getCount()==2, "Loaded sticks took another serving");
        h.assertTrue(ChopsticksItem.heldFood(player.getMainHandItem()).is(Items.APPLE), "Overwrote loaded serving");
        player.releaseUsingItem();
        box.toggleLid();
        h.assertTrue(!pickup(second, box), "Closed box allowed pickup");
        h.assertTrue(begin(player), "Closing original box prevented carried-food eating");
        finish(player);
        h.assertTrue(player.getFoodData().getFoodLevel()==4, "Could not finish carried serving");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void timedEatingLifecycle(GameTestHelper h) {
        var box = box(h);
        var player = (EatingPlayer)player(h, box);
        box.contents.setStackInSlot(0, new ItemStack(Items.APPLE));
        h.assertTrue(pickup(player, box), "Could not load timed eating test");
        player.setPos(player.getX()+10, player.getY(), player.getZ());
        var result = player.gameMode.useItem(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        h.assertTrue(result.consumesAction() && player.isUsingItem(), "Normal item-use path did not start eating");
        for (int tick=0; tick<31; tick++) player.tickEating();
        h.assertTrue(player.isUsingItem() && player.getFoodData().getFoodLevel()==0, "Food finished too soon");
        player.tickEating();
        h.assertTrue(!player.isUsingItem() && player.getFoodData().getFoodLevel()==4, "Timed eating failed to finish");
        h.assertTrue(ChopsticksItem.heldFood(player.getMainHandItem()).isEmpty(), "Timed eating left carried food");
        h.assertTrue(player.getMainHandItem().is(ModItems.CHOPSTICKS.get()), "Timed eating removed chopsticks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void returnFoodToCompartment(GameTestHelper h) {
        var box = box(h);
        var player = player(h, box);
        var apples = new ItemStack(Items.APPLE, 3);
        apples.getOrCreateTag().putString("Lunch", "keep this");
        box.contents.setStackInSlot(0, apples);
        h.assertTrue(pickup(player, box), "Could not take serving");
        h.assertTrue(pickup(player, box), "Could not return to matching stack");
        h.assertTrue(box.contents.getStackInSlot(0).getCount()==3, "Return changed total food count");
        h.assertTrue(ChopsticksItem.heldFood(player.getMainHandItem()).isEmpty(), "Return left duplicate food");
        h.assertTrue(pickup(player, box), "Could not take again");
        box.contents.setStackInSlot(0, apples.copyWithCount(64));
        h.assertTrue(!pickup(player, box), "Returned to full compartment");
        h.assertTrue(!ChopsticksItem.heldFood(player.getMainHandItem()).isEmpty(), "Full compartment lost carried food");
        box.contents.setStackInSlot(0, new ItemStack(Items.APPLE));
        h.assertTrue(!pickup(player, box), "Merged foods with different NBT");
        box.contents.setStackInSlot(0, ItemStack.EMPTY);
        h.assertTrue(pickup(player, box), "Could not return to empty compartment");
        h.assertTrue(box.contents.getStackInSlot(0).getTag().getString("Lunch").equals("keep this"), "Return lost food NBT");
        h.assertTrue(ChopsticksItem.heldFood(player.getMainHandItem()).isEmpty(), "Empty-compartment return duplicated food");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removeFoodWithoutBento(GameTestHelper h) {
        var box = box(h);
        var player = player(h, box);
        var food = new ItemStack(Items.APPLE);
        food.getOrCreateTag().putString("Lunch", "keep this");
        box.contents.setStackInSlot(0, food);
        h.assertTrue(pickup(player, box), "Could not take food for removal test");
        player.getFoodData().setFoodLevel(20);
        player.setShiftKeyDown(true);
        h.assertTrue(begin(player), "Shift-right-click did not remove serving");
        h.assertTrue(!player.isUsingItem() && player.getFoodData().getFoodLevel()==20, "Removal started eating");
        h.assertTrue(ChopsticksItem.heldFood(player.getMainHandItem()).isEmpty(), "Removal left food on chopsticks");
        h.assertTrue(player.getInventory().countItem(Items.APPLE)==1, "Removal lost/duplicated food");
        boolean preserved=false;
        for (var stack : player.getInventory().items) if (stack.is(Items.APPLE))
            preserved=stack.hasTag() && stack.getTag().getString("Lunch").equals("keep this");
        h.assertTrue(preserved, "Inventory removal lost NBT");
        player.setShiftKeyDown(false);
        box.contents.setStackInSlot(0, food.copy());
        h.assertTrue(pickup(player, box), "Could not reload for full inventory test");
        for (int slot=1; slot<36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE,64));
        player.setShiftKeyDown(true);
        h.assertTrue(begin(player), "Full inventory blocked removal");
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            player.getBoundingBox().inflate(2), entity -> entity.getItem().is(Items.APPLE));
        h.assertTrue(drops.size()==1 && ItemStack.matches(drops.get(0).getItem(), food), "Full inventory lost/duplicated dropped serving");
        h.assertTrue(ChopsticksItem.heldFood(player.getMainHandItem()).isEmpty(), "Drop left duplicate on chopsticks");
        drops.get(0).discard();
        player.setShiftKeyDown(false);
        h.succeed();
    }

}
