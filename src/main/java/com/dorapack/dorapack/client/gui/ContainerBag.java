package com.dorapack.dorapack.client.gui;

import com.dorapack.dorapack.world.PlayerBagData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * Container backing the shared four-dimensional bag. Slots are laid out in rows of 8 above the
 * player inventory. The bag handler is the shared per-UUID {@link PlayerBagData} inventory.
 */
public class ContainerBag extends Container {

    private static final int SLOTS_PER_ROW = 8;
    private static final int SLOT_SIZE = 18;
    private final int bagSlots;

    public ContainerBag(EntityPlayer player, IItemHandler bagHandler) {
        this.bagSlots = bagHandler.getSlots();
        int rows = (int) Math.ceil(bagSlots / (double) SLOTS_PER_ROW);
        int xOffset = 8 + (SLOTS_PER_ROW - Math.min(bagSlots, SLOTS_PER_ROW)) * SLOT_SIZE / 2;

        for (int i = 0; i < bagSlots; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;
            addSlotToContainer(new SlotItemHandler(bagHandler, i,
                    xOffset + col * SLOT_SIZE, 18 + row * SLOT_SIZE));
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
            boolean movedToBag = index >= bagSlots;
            if (movedToBag) {
                if (!mergeItemStack(stack, 0, bagSlots, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(stack, bagSlots, this.inventorySlots.size(), true)) {
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
