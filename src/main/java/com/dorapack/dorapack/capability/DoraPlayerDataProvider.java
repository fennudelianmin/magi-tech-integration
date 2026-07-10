package com.dorapack.dorapack.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

/**
 * Capability provider attached to each player entity, exposing a single {@link IDoraPlayerData}.
 */
public class DoraPlayerDataProvider implements ICapabilitySerializable<NBTTagCompound> {

    private final IDoraPlayerData instance = new DoraPlayerData();

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == DoraCapabilities.PLAYER_DATA;
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == DoraCapabilities.PLAYER_DATA) {
            return DoraCapabilities.PLAYER_DATA.cast(instance);
        }
        return null;
    }

    public IDoraPlayerData getInstance() {
        return instance;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return (NBTTagCompound) DoraCapabilities.PLAYER_DATA.getStorage()
                .writeNBT(DoraCapabilities.PLAYER_DATA, instance, null);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        DoraCapabilities.PLAYER_DATA.getStorage()
                .readNBT(DoraCapabilities.PLAYER_DATA, instance, null, nbt);
    }
}
