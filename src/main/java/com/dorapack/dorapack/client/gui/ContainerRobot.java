package com.dorapack.dorapack.client.gui;

import com.dorapack.dorapack.entity.EntityMiningRobot;
import com.dorapack.dorapack.robot.RobotInventoryAdapter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Container backing the mining-robot storage GUI. Exposes the robot's internal slots in rows of nine
 * above the player inventory, sized to the robot's current storage capacity.
 */
public class ContainerRobot extends Container {

    private static final int SLOTS_PER_ROW = 9;
    private static final int SLOT_SIZE = 18;
    private final int robotSlots;

    public ContainerRobot(EntityPlayer player, EntityMiningRobot robot) {
        IInventory inventory = new RobotInventoryAdapter(robot);
        this.robotSlots = inventory.getSizeInventory();
        int rows = (int) Math.ceil(robotSlots / (double) SLOTS_PER_ROW);
        for (int i = 0; i < robotSlots; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;
            addSlotToContainer(new Slot(inventory, i, 8 + col * SLOT_SIZE, 18 + row * SLOT_SIZE));
        }
        int invY = 18 + rows * SLOT_SIZE + 14;
        addPlayerInventory(player.inventory, invY);
    }

    private void addPlayerInventory(InventoryPlayer inv, int invY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inv, col + row * 9 + 9,
                        8 + col * SLOT_SIZE, invY + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inv, col, 8 + col * SLOT_SIZE, invY + 58));
        }
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        return true;
    }

    @Override
    @Nonnull
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            boolean movedToRobot = index >= robotSlots;
            if (movedToRobot) {
                if (!mergeItemStack(stack, 0, robotSlots, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(stack, robotSlots, this.inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return result;
    }
}
