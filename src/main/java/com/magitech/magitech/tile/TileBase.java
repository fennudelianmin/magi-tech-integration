package com.magitech.magitech.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public abstract class TileBase extends TileEntity implements ITickable {

    protected final ItemStackHandler itemHandler;
    protected final EnergyStorage energyStorage;

    public TileBase(int inputSlots, int outputSlots, int energyCapacity) {
        this.itemHandler = createItemHandler(inputSlots, outputSlots);
        this.energyStorage = new EnergyStorage(energyCapacity, energyCapacity > 0 ? energyCapacity / 10 : 0, 0);
    }

    protected ItemStackHandler createItemHandler(int inputSlots, int outputSlots) {
        int totalSlots = inputSlots + outputSlots;
        return new ItemStackHandler(totalSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                TileBase.this.markDirty();
            }
        };
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("items")) {
            itemHandler.deserializeNBT(compound.getCompoundTag("items"));
        }
        if (compound.hasKey("energy")) {
            energyStorage.receiveEnergy(compound.getInteger("energy"), false);
        }
        readCustomNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("items", itemHandler.serializeNBT());
        compound.setInteger("energy", energyStorage.getEnergyStored());
        writeCustomNBT(compound);
        return compound;
    }

    protected void readCustomNBT(NBTTagCompound compound) {}
    protected void writeCustomNBT(NBTTagCompound compound) {}

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        if (capability == CapabilityEnergy.ENERGY) return true;
        return hasCustomCapability(capability, facing) || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        T customCap = getCustomCapability(capability, facing);
        if (customCap != null) return customCap;
        return super.getCapability(capability, facing);
    }

    protected boolean hasCustomCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return false;
    }

    @Nullable
    protected <T> T getCustomCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        return null;
    }
}
