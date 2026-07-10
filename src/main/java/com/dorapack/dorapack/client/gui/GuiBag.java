package com.dorapack.dorapack.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Simple chest-style GUI for the four-dimensional bag. Reuses the vanilla generic chest texture
 * so no custom texture asset is required.
 */
@SideOnly(Side.CLIENT)
public class GuiBag extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    public GuiBag(EntityPlayer player, IItemHandler bagHandler) {
        super(new ContainerBag(player, bagHandler));
        this.xSize = 176;
        this.ySize = 114 + Math.min(6, (int) Math.ceil(bagHandler.getSlots() / 8.0)) * 18;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize - 96);
        drawTexturedModalRect(left, top + this.ySize - 96, 0, 126, this.xSize, 96);
    }
}
