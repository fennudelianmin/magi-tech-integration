package com.dorapack.dorapack.block.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * TileEntity for the basic anywhere door ("任意门"). Stores the owner UUID (for anti-theft checks)
 * and the bound target location of a paired door. Binding is done by shift-right-clicking two doors.
 */
public class TileAnywhereDoor extends TileEntity {

    private static final String KEY_OWNER_MOST = "OwnerMost";
    private static final String KEY_OWNER_LEAST = "OwnerLeast";
    private static final String KEY_HAS_TARGET = "HasTarget";
    private static final String KEY_TARGET_X = "TargetX";
    private static final String KEY_TARGET_Y = "TargetY";
    private static final String KEY_TARGET_Z = "TargetZ";
    private static final String KEY_TARGET_DIM = "TargetDim";

    private UUID ownerUuid;
    private boolean hasTarget;
    private BlockPos targetPos;
    private int targetDim;

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        markDirty();
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    @Nullable
    public BlockPos getTargetPos() {
        return targetPos;
    }

    public int getTargetDim() {
        return targetDim;
    }

    public void bindTo(BlockPos pos, int dimension) {
        this.targetPos = pos;
        this.targetDim = dimension;
        this.hasTarget = true;
        markDirty();
    }

    public void clearTarget() {
        this.hasTarget = false;
        this.targetPos = null;
        markDirty();
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid != null && ownerUuid.equals(uuid);
    }

    /**
     * Transient per-player teleport cooldown gate (absolute world time when ready). Not persisted:
     * cooldowns naturally reset on reload, which is acceptable and avoids NBT bloat.
     */
    private final java.util.Map<UUID, Long> cooldownReady = new java.util.HashMap<>();

    public long getCooldownReady(UUID uuid) {
        return cooldownReady.getOrDefault(uuid, 0L);
    }

    public void setCooldownReady(UUID uuid, long readyTime) {
        cooldownReady.put(uuid, readyTime);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey(KEY_OWNER_MOST)) {
            this.ownerUuid = new UUID(compound.getLong(KEY_OWNER_MOST), compound.getLong(KEY_OWNER_LEAST));
        }
        this.hasTarget = compound.getBoolean(KEY_HAS_TARGET);
        if (hasTarget) {
            this.targetPos = new BlockPos(compound.getInteger(KEY_TARGET_X),
                    compound.getInteger(KEY_TARGET_Y), compound.getInteger(KEY_TARGET_Z));
            this.targetDim = compound.getInteger(KEY_TARGET_DIM);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (ownerUuid != null) {
            compound.setLong(KEY_OWNER_MOST, ownerUuid.getMostSignificantBits());
            compound.setLong(KEY_OWNER_LEAST, ownerUuid.getLeastSignificantBits());
        }
        compound.setBoolean(KEY_HAS_TARGET, hasTarget);
        if (hasTarget && targetPos != null) {
            compound.setInteger(KEY_TARGET_X, targetPos.getX());
            compound.setInteger(KEY_TARGET_Y, targetPos.getY());
            compound.setInteger(KEY_TARGET_Z, targetPos.getZ());
            compound.setInteger(KEY_TARGET_DIM, targetDim);
        }
        return compound;
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
