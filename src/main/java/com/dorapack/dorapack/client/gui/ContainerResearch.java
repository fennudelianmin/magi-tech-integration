package com.dorapack.dorapack.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Slot-less container for the research table GUI. It exists so the vanilla GUI-open plumbing works
 * and to keep the interaction open only while the player is near the table.
 */
public class ContainerResearch extends Container {

    private final EntityPlayer player;
    private final boolean endgame;

    public ContainerResearch(EntityPlayer player, boolean endgame) {
        this.player = player;
        this.endgame = endgame;
    }

    public boolean isEndgame() {
        return endgame;
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer playerIn) {
        return playerIn == player;
    }

    @Override
    @Nonnull
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        return ItemStack.EMPTY;
    }
}
