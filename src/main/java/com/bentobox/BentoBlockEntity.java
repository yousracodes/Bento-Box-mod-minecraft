package com.bentobox;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public final class BentoBlockEntity extends BlockEntity implements MenuProvider {
    private ItemStack box = ItemStack.EMPTY;
    private boolean loading;
    public final ItemStackHandler contents;
    public BentoBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.BENTO.get(), pos, state);
        contents = new ItemStackHandler(((BentoBlock)state.getBlock()).compartments()) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return validFood(stack); }
            @Override protected void onContentsChanged(int slot) { if (!loading) changed(); }
        };
    }
    public static boolean validFood(ItemStack stack) { return !stack.isEmpty() && !(stack.getItem() instanceof BentoBoxItem) && stack.isEdible(); }
    public void setBox(ItemStack stack) {
        box = stack.copy();
        box.setCount(1);
        loading = true;
        loadContents();
        loading = false;
        changed();
    }
    private ItemStack storedBox() {
        if (box.isEmpty()) box = new ItemStack(getBlockState().getBlock().asItem());
        return box;
    }
    private void loadContents() {
        var data = box.getOrCreateTag().getCompound("BentoContents").copy();
        data.putInt("Size", ((BentoBlock)getBlockState().getBlock()).compartments());
        contents.deserializeNBT(data);
    }
    public ItemStack boxStack() {
        ItemStack result = storedBox().copy();
        result.getOrCreateTag().put("BentoContents", contents.serializeNBT());
        return result;
    }
    public float openness(double time) { return BentoBoxItem.openness(storedBox(), time); }
    public boolean isLidOpen() { return storedBox().getOrCreateTag().getBoolean(BentoBoxItem.OPEN); }
    public void toggleLid() {
        var stack = storedBox();
        float from = BentoBoxItem.openness(stack, level.getGameTime());
        var tag = stack.getOrCreateTag();
        tag.putFloat(BentoBoxItem.FROM, from);
        tag.putLong(BentoBoxItem.CHANGED, level.getGameTime());
        tag.putBoolean(BentoBoxItem.OPEN, !tag.getBoolean(BentoBoxItem.OPEN));
        changed();
    }
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Box", boxStack().save(new CompoundTag())); }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        loading = true;
        box = ItemStack.of(tag.getCompound("Box"));
        loadContents();
        loading = false;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public Component getDisplayName() { return Component.translatable("container.bentobox.bento"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new BentoMenu(id, inventory, this); }
}
