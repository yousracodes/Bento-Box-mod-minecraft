package com.bentobox.client;

import com.bentobox.BentoMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla container frame and slots, with the bento's pixel decorations retained. */
public final class BentoScreen extends AbstractContainerScreen<BentoMenu> {
    private static final ResourceLocation CONTAINER = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    public BentoScreen(BentoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = menu.inventoryY() + 83;
        titleLabelX = 32;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = menu.inventoryY() - 11;
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        int trayHeight = (menu.compartments / menu.columns()) * 18;
        // Use Minecraft's own frame, inset slots, inventory, and hotbar textures.
        g.blit(CONTAINER, x, y, 0, 0, imageWidth, 17);
        g.fill(x+7, y+17, x+169, y+17+trayHeight, 0xFFC6C6C6);
        g.blit(CONTAINER, x, y+17, 0, 17, 7, trayHeight);
        g.blit(CONTAINER, x+169, y+17, 169, 17, 7, trayHeight);
        g.blit(CONTAINER, x, y+17+trayHeight, 0, 126, imageWidth, 96);
        for (int slot = 0; slot < menu.compartments; slot++)
            g.blit(CONTAINER, x+menu.foodSlotX(slot)-1, y+menu.foodSlotY(slot)-1, 7, 17, 18, 18);

        // Small bento emblem beside the title.
        g.fill(x+8,y+5,x+27,y+16,0xFF6D393D);
        g.fill(x+10,y+3,x+25,y+14,0xFFC47779);
        g.fill(x+12,y+5,x+23,y+10,0xFFF8EBCF);
        g.fill(x+13,y+5,x+17,y+9,0xFF91A17B);
        g.fill(x+19,y+7,x+23,y+10,0xFFD69A98);
        // Cherry branch and blossoms stay outside the slot grid.
        for (int i=0;i<5;i++) g.fill(x+137+i*4,y+17-i*2,x+143+i*4,y+19-i*2,0xFF765449);
        flower(g,x+141,y+10);
        flower(g,x+155,y+4);
        flower(g,x+23,y+25);
        flower(g,x+146,y+25);

        // Cat and rice ball sit alongside the bottom row of food slots.
        int base = y+17+trayHeight-5;
        g.fill(x+140,base-12,x+146,base-8,0xFF80645D);
        g.fill(x+136,base-8,x+150,base+2,0xFF80645D);
        g.fill(x+138,base-7,x+148,base,0xFFFFF0D9);
        g.fill(x+141,base-3,x+146,base+2,0xFF39403B);
        g.fill(x+20,base-9,x+24,base-2,0xFF80645D);
        g.fill(x+33,base-9,x+37,base-2,0xFF80645D);
        g.fill(x+20,base-3,x+37,base+3,0xFFFFE4CE);
        g.fill(x+24,base,x+26,base+2,0xFF59403D);
        g.fill(x+31,base,x+33,base+2,0xFF59403D);
    }

    private void flower(GuiGraphics g, int x, int y) {
        g.fill(x+2,y,x+6,y+8,0xFFE6A4B4);
        g.fill(x,y+2,x+8,y+6,0xFFE6A4B4);
        g.fill(x+3,y+3,x+5,y+5,0xFFFFD8D5);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g,mouseX,mouseY,partialTick);
        renderTooltip(g,mouseX,mouseY);
    }
}
