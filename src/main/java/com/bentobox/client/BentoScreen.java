package com.bentobox.client;

import com.bentobox.BentoMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Pixel-drawn cherry stationery panel inspired by the supplied GUI reference. */
public final class BentoScreen extends AbstractContainerScreen<BentoMenu> {
    public BentoScreen(BentoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 230;
        imageHeight = menu.inventoryY() + 98;
        titleLabelX = 40;
        titleLabelY = 12;
        inventoryLabelX = 15;
        inventoryLabelY = menu.inventoryY() - 12;
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x=leftPos, y=topPos;
        g.fill(x-2,y+2,x+imageWidth+2,y+imageHeight-2,0xFF322C30);
        g.fill(x+2,y-2,x+imageWidth-2,y+imageHeight+2,0xFF322C30);
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFFF8F0EB);
        g.fill(x+3,y+3,x+imageWidth-3,y+imageHeight-3,0xFFE5DAD8);
        g.fill(x+3,y+3,x+imageWidth-3,y+5,0xFFFFFFFF);
        g.fill(x+imageWidth-5,y+5,x+imageWidth-3,y+imageHeight-3,0xFFB4A3AA);
        // Tiny lunchbox emblem.
        g.fill(x+13,y+12,x+33,y+26,0xFF6D393D);
        g.fill(x+15,y+10,x+31,y+23,0xFFC47779);
        g.fill(x+17,y+12,x+29,y+18,0xFFF8EBCF);
        g.fill(x+19,y+12,x+23,y+16,0xFF91A17B);
        g.fill(x+25,y+14,x+29,y+18,0xFFD69A98);
        // Cherry branch and pixel blossoms.
        for(int i=0;i<6;i++) g.fill(x+190+i*4,y+25-i*3,x+196+i*4,y+28-i*3,0xFF765449);
        flower(g,x+194,y+18); flower(g,x+210,y+10); flower(g,x+204,y+29);
        flower(g,x+25,y+65); flower(g,x+202,y+73);
        for(int i=0;i<menu.compartments;i++) slot(g,x+70+(i%3)*30,y+25+(i/3)*30,30);
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) slot(g,x+15+col*22,y+menu.inventoryY()-2+row*22,20);
        for(int col=0;col<9;col++) slot(g,x+15+col*22,y+menu.inventoryY()+70,20);
        // Rice ball and cat ears sit beside the food compartments.
        int base=y+menu.inventoryY()-20;
        g.fill(x+192,base-12,x+198,base-8,0xFF80645D);
        g.fill(x+188,base-8,x+202,base+2,0xFF80645D);
        g.fill(x+190,base-7,x+200,base,0xFFFFF0D9);
        g.fill(x+193,base-3,x+198,base+2,0xFF39403B);
        g.fill(x+20,base-9,x+24,base-2,0xFF80645D);
        g.fill(x+33,base-9,x+37,base-2,0xFF80645D);
        g.fill(x+20,base-3,x+37,base+3,0xFFFFE4CE);
        g.fill(x+24,base,x+26,base+2,0xFF59403D);
        g.fill(x+31,base,x+33,base+2,0xFF59403D);
    }
    private void slot(GuiGraphics g,int x,int y,int size) {
        g.fill(x,y,x+size,y+size,0xFF807778);
        g.fill(x+1,y+1,x+size-1,y+size-1,0xFFAAA0A0);
        g.fill(x+2,y+2,x+size-1,y+size-1,0xFFBEB4B0);
        g.fill(x+1,y+size-1,x+size,y+size,0xFFFFF6F0);
        g.fill(x+size-1,y+1,x+size,y+size,0xFFFFF6F0);
    }
    private void flower(GuiGraphics g,int x,int y) {
        g.fill(x+2,y,x+6,y+8,0xFFE6A4B4);
        g.fill(x,y+2,x+8,y+6,0xFFE6A4B4);
        g.fill(x+3,y+3,x+5,y+5,0xFFFFD8D5);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        renderBackground(g);
        super.render(g,mouseX,mouseY,partialTick);
        renderTooltip(g,mouseX,mouseY);
    }
}
