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

/**
 * 机器方块的 TileEntity 基类。
 * <p>
 * 为本模组所有机器提供统一的物品栏（ItemStackHandler）和能量存储（EnergyStorage）基础设施，
 * 并实现 Forge 的 Capability 系统暴露、NBT 持久化、以及每 tick 更新接口。
 * <p>
 * 子类只需关注自身的机器逻辑，无需重复处理物品/能量的 Capability 注册和序列化。
 */
public abstract class TileBase extends TileEntity implements ITickable {

    /** 通用物品栏处理器，前半部分为输入槽，后半部分为输出槽 */
    protected final ItemStackHandler itemHandler;

    /** 通用能量存储 */
    protected final EnergyStorage energyStorage;

    /**
     * 构造一个基础机器 TileEntity。
     *
     * @param inputSlots     输入槽数量
     * @param outputSlots    输出槽数量
     * @param energyCapacity 最大能量容量（FE），设为 0 表示不需要能量
     */
    public TileBase(int inputSlots, int outputSlots, int energyCapacity) {
        this.itemHandler = createItemHandler(inputSlots, outputSlots);
        // 能量接收速率 = 容量 / 10，不允许对外输出能量（extract=0）
        this.energyStorage = new EnergyStorage(energyCapacity, energyCapacity > 0 ? energyCapacity / 10 : 0, 0);
    }

    /**
     * 创建物品栏处理器，总槽位 = 输入槽 + 输出槽。
     * 当任意槽位内容变更时自动调用 markDirty() 以触发世界保存。
     */
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

    /**
     * 从 NBT 读取数据，恢复物品栏、能量值，再调用子类的自定义读取逻辑。
     */
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("items")) {
            itemHandler.deserializeNBT(compound.getCompoundTag("items"));
        }
        if (compound.hasKey("energy")) {
            // receiveEnergy 会把能量值设置到存储中
            energyStorage.receiveEnergy(compound.getInteger("energy"), false);
        }
        readCustomNBT(compound);
    }

    /**
     * 将物品栏、能量值写入 NBT，再调用子类的自定义写入逻辑。
     */
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("items", itemHandler.serializeNBT());
        compound.setInteger("energy", energyStorage.getEnergyStored());
        writeCustomNBT(compound);
        return compound;
    }

    /** 子类可覆写以读写额外的 NBT 数据（如魔力、进度等） */
    protected void readCustomNBT(NBTTagCompound compound) {}
    protected void writeCustomNBT(NBTTagCompound compound) {}

    /**
     * 暴露物品栏和能量的 Capability，供自动化设备（管道、漏斗）访问。
     * 子类可通过覆写 hasCustomCapability 添加更多 Capability（如流体）。
     */
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

    /** 子类可覆写以声明额外的 Capability 类型 */
    protected boolean hasCustomCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return false;
    }

    /** 子类可覆写以返回额外的 Capability 实例 */
    @Nullable
    protected <T> T getCustomCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        return null;
    }
}
