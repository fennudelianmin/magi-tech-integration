package com.dorapack.dorapack.block.tile;

import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.entity.EntityMiningRobot;
import ic2.api.energy.prefab.BasicSink;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * Charging station ("充电站") TileEntity. Acts as an IC2 energy sink (via {@link BasicSink}), buffering
 * up to {@link DoraConfig#chargingStationCapacity} EU and dispensing {@link DoraConfig#chargingStationRate}
 * EU/t into any mining robots standing within {@link #CHARGE_RADIUS} blocks.
 *
 * <p>Only a bounded local search is used to find robots, per the design document's performance rules.
 * The {@link BasicSink} handles all IC2 net registration/NBT; we forward the standard TE lifecycle to
 * it and pull stored energy out to charge robots each tick.</p>
 */
public class TileChargingStation extends TileEntity implements ITickable {

    private static final double CHARGE_RADIUS = 4.0D;

    private BasicSink energySink;

    private BasicSink sink() {
        if (energySink == null) {
            energySink = new BasicSink(this, DoraConfig.chargingStationCapacity, 3);
        }
        return energySink;
    }

    @Override
    public void onLoad() {
        if (!world.isRemote) {
            sink().onLoad();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (energySink != null && world != null && !world.isRemote) {
            energySink.invalidate();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (energySink != null && world != null && !world.isRemote) {
            energySink.onChunkUnload();
        }
    }

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        BasicSink sink = sink();
        sink.update();
        int rate = DoraConfig.chargingStationRate;
        if (!sink.canUseEnergy(rate)) {
            return;
        }
        AxisAlignedBB area = new AxisAlignedBB(getPos()).grow(CHARGE_RADIUS);
        List<EntityMiningRobot> robots = world.getEntitiesWithinAABB(EntityMiningRobot.class, area);
        for (EntityMiningRobot robot : robots) {
            if (!sink.canUseEnergy(rate)) {
                break;
            }
            int accepted = robot.addEnergy(rate);
            if (accepted > 0) {
                sink.useEnergy(accepted);
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        sink().readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        sink().writeToNBT(compound);
        return compound;
    }
}
