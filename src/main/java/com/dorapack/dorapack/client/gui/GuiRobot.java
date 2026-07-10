package com.dorapack.dorapack.client.gui;

import com.dorapack.dorapack.entity.EntityMiningRobot;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Storage GUI for the mining robot. Reuses the vanilla generic chest texture and overlays the robot's
 * current energy so the owner can see charge state at a glance.
 */
@SideOnly(Side.CLIENT)
public class GuiRobot extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    private final EntityMiningRobot robot;

    public GuiRobot(EntityPlayer player, EntityMiningRobot robot) {
        super(new ContainerRobot(player, robot));
        this.robot = robot;
        this.xSize = 176;
        int rows = Math.min(6, (int) Math.ceil(robot.getInventory().size() / 9.0));
        this.ySize = 114 + rows * 18;
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

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String energy = "EU: " + robot.getEnergy() + " / " + robot.getStats().getMaxEnergy();
        this.fontRenderer.drawString(energy, 8, 6, 0x404040);
    }
}
