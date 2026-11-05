package com.bentobox;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;

public final class BentoMenu extends AbstractContainerMenu {
    public final int compartments;
    private final BentoBlockEntity bento;
    private final BlockPos pos;
    private final DataSlot decorationMode = DataSlot.standalone();
    public boolean decorating() { return decorationMode.get() != 0; }
    public ItemStack decorationAt(int slot) { return slots.get(compartments + slot).getItem(); }
    public int columns() { return compartments == 4 ? 2 : 3; }
    public int foodSlotX(int slot) { return (176 - columns() * 18) / 2 + 1 + (slot % columns()) * 18; }
    public int foodSlotY(int slot) { return 38 + (slot / columns()) * 18; }
    public int inventoryY() { return 51 + (compartments / columns()) * 18; }
    public BentoMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos(), data.readVarInt(), null);
    }
    public BentoMenu(int id, Inventory inventory, BentoBlockEntity bento) {
        this(id, inventory, bento.getBlockPos(), bento.contents.getSlots(), bento);
    }
    private BentoMenu(int id, Inventory inventory, BlockPos pos, int size, BentoBlockEntity bento) {
        super(ModContent.MENU.get(), id);
        if (size != 4 && size != 6 && size != 9) throw new IllegalArgumentException("Invalid bento capacity");
        this.compartments = size;
        this.bento = bento;
        this.pos = pos;
        addDataSlot(decorationMode);
        IItemHandler handler = bento == null ? new ItemStackHandler(size) : bento.contents;
        for (int i=0; i<size; i++) addSlot(new SlotItemHandler(handler, i, foodSlotX(i), foodSlotY(i)) {
            @Override public boolean isActive() { return !decorating(); }
            @Override public boolean mayPlace(ItemStack stack) { return isActive() && BentoBlockEntity.validFood(stack); }
            @Override public boolean mayPickup(Player player) { return isActive(); }
            @Override public void setChanged() {
                super.setChanged();
                // Vanilla transfers may grow a stack directly rather than call insertItem.
                if (bento != null) bento.changed();
            }
        });
        IItemHandler picks = bento == null ? new ItemStackHandler(size) : bento.decorations;
        for (int i=0; i<size; i++) addSlot(new SlotItemHandler(picks, i, foodSlotX(i), foodSlotY(i)) {
            @Override public boolean isActive() { return decorating(); }
            @Override public boolean mayPlace(ItemStack stack) { return isActive() && stack.getItem() instanceof FoodPickItem; }
            @Override public boolean mayPickup(Player player) { return isActive(); }
            @Override public int getMaxStackSize() { return 1; }
            @Override public int getMaxStackSize(ItemStack stack) { return 1; }
            @Override public void setChanged() {
                super.setChanged();
                if (bento != null) bento.changed();
            }
        });
        for (int row=0; row<3; row++) for (int col=0; col<9; col++)
            addSlot(new Slot(inventory, col+row*9+9, 8+col*18, inventoryY()+row*18));
        for (int col=0; col<9; col++) addSlot(new Slot(inventory, col, 8+col*18, inventoryY()+58));
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !stillValid(player) || !getCarried().isEmpty()) return false;
        decorationMode.set(decorating() ? 0 : 1);
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player) {
        return bento == null || (!bento.isRemoved() && player.level().getBlockEntity(pos) == bento && player.distanceToSqr(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)<=64);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = compartments * 2;
        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean valid = decorating() ? stack.getItem() instanceof FoodPickItem : BentoBlockEntity.validFood(stack);
            int start = decorating() ? compartments : 0;
            if (!valid || !moveItemStackTo(stack, start, start + compartments, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot >= 0 && slot < compartments * 2 && !slots.get(slot).isActive()) return;
        if (stillValid(player)) super.clicked(slot, button, type, player);
    }
}
