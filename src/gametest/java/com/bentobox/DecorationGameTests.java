package com.bentobox;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder(BentoBoxMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DecorationGameTests {
    @GameTest(template = "empty")
    public static void compactPicks(GameTestHelper h) { persistence(h, ModContent.COMPACT.get()); }
    @GameTest(template = "empty")
    public static void classicPicks(GameTestHelper h) { persistence(h, ModContent.CLASSIC.get()); }
    @GameTest(template = "empty")
    public static void deluxePicks(GameTestHelper h) { persistence(h, ModContent.DELUXE.get()); }

    private static void persistence(GameTestHelper h, Block block) {
        var pos = h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        var box = (BentoBlockEntity)h.getLevel().getBlockEntity(pos);
        int count = box.contents.getSlots();
        h.assertTrue(box.decorations.getSlots()==count, "Decoration capacity does not match food");
        h.assertTrue(box.decorations.insertItem(0,new ItemStack(Items.APPLE),false).getCount()==1, "Decoration slot accepted food");
        h.assertTrue(box.contents.insertItem(0,new ItemStack(ModItems.BUNNY_PICK.get()),false).getCount()==1, "Food slot accepted a pick");
        for (int slot=0; slot<count; slot++) {
            var pick = new ItemStack(ModItems.FOOD_PICKS.get(slot % 7).get(), 2);
            pick.getOrCreateTag().putInt("PersonalPick", slot);
            h.assertTrue(box.decorations.insertItem(slot,pick,false).getCount()==1, "Decoration slot did not limit count to one");
        }
        var apples = new ItemStack(Items.APPLE, 3);
        box.contents.setStackInSlot(0, apples);
        var saved = box.saveWithoutMetadata();
        var restored = new BentoBlockEntity(pos, block.defaultBlockState());
        restored.load(saved);
        for (int slot=0; slot<count; slot++)
            h.assertTrue(ItemStack.matches(box.decorations.getStackInSlot(slot), restored.decorations.getStackInSlot(slot)), "Save/load lost pick/NBT");
        restored.decorations.extractItem(0,1,false);
        h.assertTrue(!box.decorations.getStackInSlot(0).isEmpty(), "Boxes shared decoration storage");
        box.contents.extractItem(0,3,false);
        h.assertTrue(!box.decorations.getStackInSlot(0).isEmpty(), "Removing food consumed the pick");
        var oldBox = new ItemStack(block.asItem());
        oldBox.getOrCreateTag().put("BentoContents", box.contents.serializeNBT());
        restored.setBox(oldBox);
        for (int slot=0; slot<count; slot++) h.assertTrue(restored.decorations.getStackInSlot(slot).isEmpty(), "Legacy box inherited decorations");
        h.getLevel().removeBlock(pos, false);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1), e -> e.getItem().is(block.asItem()));
        h.assertTrue(drops.size()==1, "Decorated box did not drop exactly once");
        restored.setBox(drops.get(0).getItem());
        h.assertTrue(restored.decorations.getStackInSlot(count-1).getTag().getInt("PersonalPick")==count-1, "Drop/replacement lost last pick");
        drops.get(0).discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void decorationMenuTransfers(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlock(pos, ModContent.CLASSIC.get().defaultBlockState(), 3);
        var box = (BentoBlockEntity)h.getLevel().getBlockEntity(pos);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "Decorations"));
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        player.getInventory().clearContent();
        var menu = new BentoMenu(1, player.getInventory(), box);
        int count = menu.compartments, playerStart = count*2;
        box.contents.setStackInSlot(0,new ItemStack(Items.APPLE,4));
        player.getInventory().setItem(9,new ItemStack(ModItems.BUNNY_PICK.get(),3));
        h.assertTrue(menu.quickMoveStack(player,playerStart).isEmpty(), "Food mode accepted picks");
        h.assertTrue(menu.clickMenuButton(player,0) && menu.decorating(), "Decoration mode did not open");
        h.assertTrue(!menu.getSlot(0).isActive() && menu.getSlot(count).isActive(), "Incorrect active slots");
        menu.clicked(playerStart,0,ClickType.QUICK_MOVE,player);
        for (int slot=0; slot<3; slot++) h.assertTrue(box.decorations.getStackInSlot(slot).getCount()==1, "Shift-click did not distribute one pick per slot");
        h.assertTrue(player.getInventory().getItem(9).isEmpty(), "Shift-click did not consume transferred picks");
        h.assertTrue(menu.quickMoveStack(player,0).isEmpty(), "Hidden food shifted out");
        menu.clicked(0,0,ClickType.PICKUP,player);
        h.assertTrue(menu.getCarried().isEmpty() && box.contents.getStackInSlot(0).getCount()==4, "Hidden food slot was clickable");
        menu.quickMoveStack(player,count);
        h.assertTrue(box.decorations.getStackInSlot(0).isEmpty(), "Could not remove pick with shift-click");
        player.getInventory().setItem(9,new ItemStack(Items.APPLE));
        h.assertTrue(menu.quickMoveStack(player,playerStart).isEmpty(), "Decoration mode accepted food");
        menu.setCarried(new ItemStack(ModItems.STAR_PICK.get()));
        h.assertTrue(!menu.clickMenuButton(player,0), "Mode changed with occupied cursor");
        menu.setCarried(ItemStack.EMPTY);
        h.assertTrue(menu.clickMenuButton(player,0) && !menu.decorating(), "Could not return to food mode");
        menu.clicked(count+1,0,ClickType.PICKUP,player);
        h.assertTrue(menu.getCarried().isEmpty() && !box.decorations.getStackInSlot(1).isEmpty(), "Hidden pick slot was clickable");
        h.assertTrue(!menu.clickMenuButton(player,99), "Accepted an invalid button");
        var otherMenu = new BentoMenu(2,player.getInventory(),box);
        menu.clickMenuButton(player,0);
        h.assertTrue(!otherMenu.decorating(), "Mode leaked across open menus");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void allPickRecipes(GameTestHelper h) {
        var ingredients = new Item[][]{
            {Items.STICK,Items.WHITE_WOOL,Items.PINK_DYE}, {Items.STICK,Items.BROWN_WOOL},
            {Items.STICK,Items.RED_WOOL}, {Items.STICK,Items.GOLD_NUGGET},
            {Items.STICK,Items.LIME_DYE,Items.WHEAT_SEEDS}, {Items.STICK,Items.RED_DYE,Items.POPPY},
            {Items.STICK,Items.PINK_PETALS}
        };
        var dummy = new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int slot) { return ItemStack.EMPTY; }
        };
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(dummy,3,3);
        for (int design=0; design<ingredients.length; design++) {
            grid.clearContent();
            for (int slot=0; slot<ingredients[design].length; slot++) grid.setItem(slot,new ItemStack(ingredients[design][slot]));
            var recipe = h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel());
            h.assertTrue(recipe.isPresent(), "Missing pick recipe " + design);
            ItemStack output = recipe.get().assemble(grid,h.getLevel().registryAccess());
            h.assertTrue(output.is(ModItems.FOOD_PICKS.get(design).get()) && output.getCount()==1, "Wrong pick recipe result " + design);
        }
        h.succeed();
    }
}
