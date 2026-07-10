package com.dorapack.dorapack.block.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Network core ("网络核心") TileEntity for the anywhere-door hub multiblock. Stores the owner and a
 * round-robin selection index used to cycle between the frame doors' bound targets when the core is
 * activated. Target discovery is performed by a bounded local scan in the block class, not persisted,
 * so this tile only holds the small amount of state that must survive reloads.
 */
public class TileDoorHubCore extends TileEntity {

    private static final String KEY_OWNER_MOST = "OwnerMost";
    private static final String KEY_OWNER_LEAST = "OwnerLeast";
    private static final String KEY_INDEX = "SelectionIndex";

    private UUID ownerUuid;
    private int selectionIndex;

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        markDirty();
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid != null && ownerUuid.equals(uuid);
    }

    public int getSelectionIndex() {
        return selectionIndex;
    }

    /** Advances the selection index within {@code count} slots (wrapping), returning the new value. */
    public int advanceSelection(int count) {
        if (count <= 0) {
            selectionIndex = 0;
        } else {
            selectionIndex = (selectionIndex + 1) % count;
        }
        markDirty();
        return selectionIndex;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        if (ownerUuid != null) {
            compound.setLong(KEY_OWNER_MOST, ownerUuid.getMostSignificantBits());
            compound.setLong(KEY_OWNER_LEAST, ownerUuid.getLeastSignificantBits());
        }
        compound.setInteger(KEY_INDEX, selectionIndex);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey(KEY_OWNER_MOST)) {
            this.ownerUuid = new UUID(compound.getLong(KEY_OWNER_MOST), compound.getLong(KEY_OWNER_LEAST));
        }
        this.selectionIndex = compound.getInteger(KEY_INDEX);
    }
}
