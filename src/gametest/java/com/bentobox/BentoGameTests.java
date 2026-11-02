package com.bentobox;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder(BentoBoxMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BentoGameTests {
    @GameTest(template = "empty")
    public static void classic(GameTestHelper helper) { check(helper, 6); }
    @GameTest(template = "empty")
    public static void deluxe(GameTestHelper helper) { check(helper, 9); }

    private static void check(GameTestHelper h, int count) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1,1,1));
        var block = count == 6 ? ModContent.CLASSIC.get() : ModContent.DELUXE.get();
        level.setBlock(pos, block.defaultBlockState(), 3);
        var box = (BentoBlockEntity)level.getBlockEntity(pos);
        ItemStack namedBox = new ItemStack(block.asItem());
        namedBox.setHoverName(net.minecraft.network.chat.Component.literal("My lunch"));
        box.setBox(namedBox);
        h.assertTrue(box.contents.getSlots() == count, "Wrong compartment count");
        h.assertTrue(box.contents.insertItem(0,new ItemStack(Items.STONE),false).getCount()==1,"Accepted non-food");
        h.assertTrue(box.contents.insertItem(0,new ItemStack(block.asItem()),false).getCount()==1,"Accepted nested bento");
        ItemStack food = new ItemStack(Items.APPLE,32);
        food.setHoverName(net.minecraft.network.chat.Component.literal("Picnic apples"));
        food.getOrCreateTag().putString("CustomFoodData","preserve me");
        box.contents.insertItem(0,food,false);
        box.contents.insertItem(count-1,new ItemStack(Items.MUSHROOM_STEW),false);
        h.assertTrue(!box.contents.insertItem(count-1,new ItemStack(Items.MUSHROOM_STEW),false).isEmpty(),"Exceeded stew stack limit");
        box.toggleLid();
        h.assertTrue(box.boxStack().getTag().getBoolean(BentoBoxItem.OPEN),"Lid did not open");
        var saved = box.saveWithoutMetadata();
        var restored = new BentoBlockEntity(pos,block.defaultBlockState());
        restored.load(saved);
        h.assertTrue(ItemStack.matches(restored.contents.getStackInSlot(0),food),"Save/load lost food data");
        restored.contents.setStackInSlot(0,ItemStack.EMPTY);
        h.assertTrue(ItemStack.matches(box.contents.getStackInSlot(0),food),"Boxes shared an inventory");
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
        player.getInventory().clearContent();
        var menu = new BentoMenu(1,player.getInventory(),box);
        menu.quickMoveStack(player,0);
        h.assertTrue(box.contents.getStackInSlot(0).isEmpty(),"Shift-click did not extract food");
        int inventorySlot=-1;
        for(int i=count;i<menu.slots.size();i++) if(ItemStack.matches(menu.getSlot(i).getItem(),food)) inventorySlot=i;
        h.assertTrue(inventorySlot>=0,"Shift-click lost food data");
        menu.quickMoveStack(player,inventorySlot);
        h.assertTrue(ItemStack.matches(box.contents.getStackInSlot(0),food),"Shift-click did not return food");
        for(int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        h.assertTrue(menu.quickMoveStack(player,0).isEmpty(),"Transferred food into full inventory");
        h.assertTrue(ItemStack.matches(box.contents.getStackInSlot(0),food),"Full inventory lost food");
        player.getInventory().clearContent();
        block.attack(block.defaultBlockState(),level,pos,player);
        h.assertTrue(level.isEmptyBlock(pos),"Click did not remove box");
        h.assertTrue(!menu.stillValid(player),"Removed box menu stayed valid");
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1));
        h.assertTrue(drops.size()==1,"Expected exactly one box drop");
        h.assertTrue(drops.get(0).getItem().getHoverName().getString().equals("My lunch"),"Drop lost box name");
        var picked = new BentoBlockEntity(pos,block.defaultBlockState());
        picked.setBox(drops.get(0).getItem());
        h.assertTrue(picked.contents.getSlots()==count && ItemStack.matches(picked.contents.getStackInSlot(0),food),"Drop lost capacity or food NBT");
        h.assertTrue(picked.contents.getStackInSlot(count-1).is(Items.MUSHROOM_STEW),"Drop lost last compartment");
        drops.get(0).discard();
        h.succeed();
    }
}
