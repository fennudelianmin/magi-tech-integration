package com.dorapack.dorapack.robot;

import com.dorapack.dorapack.entity.EntityMiningRobot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;

/**
 * {@link IInventory} view over a mining robot's internal storage, letting the robot GUI container
 * bind to the entity's slot list without the entity itself implementing the whole interface.
 */
public class RobotInventoryAdapter implements IInventory {

    private static final int STACK_LIMIT = 64;
    /** Squared reach (8 blocks) beyond which the GUI auto-closes. */
    private static final double MAX_INTERACT_DIST_SQ = 64.0D;

    private final EntityMiningRobot robot;

    public RobotInventoryAdapter(EntityMiningRobot robot) {
        this.robot = robot;
    }

    @Override
    public int getSizeInventory() {
        return robot.getInventory().size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : robot.getInventory()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int index) {
        return robot.getInventory().get(index);
    }

    @Override
    @Nonnull
    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndSplit(robot.getInventory(), index, count);
    }

    @Override
    @Nonnull
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(robot.getInventory(), index);
    }

    @Override
    public void setInventorySlotContents(int index, @Nonnull ItemStack stack) {
        robot.getInventory().set(index, stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return STACK_LIMIT;
    }

    @Override
    public void markDirty() {
        // Entity NBT is saved on world save; nothing to flush eagerly.
    }

    @Override
    public boolean isUsableByPlayer(@Nonnull EntityPlayer player) {
        return robot.isEntityAlive() && robot.getDistanceSq(player) <= MAX_INTERACT_DIST_SQ;
    }

    @Override
    public void openInventory(@Nonnull EntityPlayer player) {
        // No-op.
    }

    @Override
    public void closeInventory(@Nonnull EntityPlayer player) {
        // No-op.
    }

    @Override
    public boolean isItemValidForSlot(int index, @Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return robot.getEnergy();
    }

    @Override
    public void setField(int id, int value) {
        robot.setEnergy(value);
    }

    @Override
    public int getFieldCount() {
        return 1;
    }

    @Override
    public void clear() {
        robot.getInventory().clear();
    }

    @Override
    @Nonnull
    public String getName() {
        return "container.dorapack.mining_robot";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    @Nonnull
    public ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }
}
