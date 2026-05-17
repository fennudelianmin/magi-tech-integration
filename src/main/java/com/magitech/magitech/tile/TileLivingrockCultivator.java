package com.magitech.magitech.tile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileLivingrockCultivator extends TileEntity implements ITickable {

    // 基础参数
    public static final int ENERGY_CAPACITY = 10000;
    public static final int ENERGY_PER_TICK = 4;          // 每个处理槽位每tick耗能
    public static final int PROCESS_TIME = 100;           // 5秒 = 100 ticks (20 ticks/秒)
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOTS = 4;
    public static final int PROCESS_SLOTS = 8;            // 同时处理8个

    // 自定义能量存储，支持 NBT
    private static class CustomEnergyStorage extends EnergyStorage {
        public CustomEnergyStorage(int capacity) {
            super(capacity);
        }

        public CustomEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }

        public void readFromNBT(NBTTagCompound compound) {
            if (compound.hasKey("Energy")) {
                setEnergy(compound.getInteger("Energy"));
            }
        }

        public void writeToNBT(NBTTagCompound compound) {
            compound.setInteger("Energy", getEnergyStored());
        }
    }

    // 内部数据结构：处理槽位
    private static class ProcessSlot {
        ItemStack input = ItemStack.EMPTY;   // 正在处理的原料副本
        int progress = 0;                     // 0 ~ PROCESS_TIME

        NBTTagCompound serialize() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("Input", input.serializeNBT());
            tag.setInteger("Progress", progress);
            return tag;
        }

        void deserialize(NBTTagCompound tag) {
            input = new ItemStack(tag.getCompoundTag("Input"));
            progress = tag.getInteger("Progress");
        }
    }

    private final ProcessSlot[] processSlots = new ProcessSlot[PROCESS_SLOTS];
    private final ItemStackHandler inputHandler = new ItemStackHandler(INPUT_SLOTS);
    private final ItemStackHandler outputHandler = new ItemStackHandler(OUTPUT_SLOTS);
    private final CustomEnergyStorage energyStorage = new CustomEnergyStorage(ENERGY_CAPACITY, ENERGY_CAPACITY, ENERGY_CAPACITY);

    public TileLivingrockCultivator() {
        for (int i = 0; i < PROCESS_SLOTS; i++) {
            processSlots[i] = new ProcessSlot();
        }
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        boolean changed = false;

        // 1. 能量消耗与进度推进
        int activeSlots = 0;
        for (ProcessSlot slot : processSlots) {
            if (!slot.input.isEmpty() && slot.progress < PROCESS_TIME) activeSlots++;
        }
        int energyRequired = activeSlots * ENERGY_PER_TICK;
        if (energyStorage.getEnergyStored() >= energyRequired && energyRequired > 0) {
            energyStorage.extractEnergy(energyRequired, false);
            // 进度增加
            for (ProcessSlot slot : processSlots) {
                if (!slot.input.isEmpty() && slot.progress < PROCESS_TIME) {
                    slot.progress++;
                    changed = true;
                }
            }
        }

        // 2. 处理已完成的任务（progress >= PROCESS_TIME）
        for (int i = 0; i < PROCESS_SLOTS; i++) {
            ProcessSlot slot = processSlots[i];
            if (!slot.input.isEmpty() && slot.progress >= PROCESS_TIME) {
                // 尝试输出
                ItemStack output = getOutputForInput(slot.input);
                if (!output.isEmpty() && tryInsertOutput(output)) {
                    // 输出成功，清空处理槽，尝试从输入槽取新原料
                    slot.input = ItemStack.EMPTY;
                    slot.progress = 0;
                    changed = true;
                    // 立即补充新任务（如果有原料）
                    tryFillProcessSlot(i);
                }
                // 如果输出失败（输出槽满），则保持完成状态，等待下次tick再试
            }
        }

        // 3. 补充空处理槽（如果输出成功导致槽位空，或者一开始就是空的）
        for (int i = 0; i < PROCESS_SLOTS; i++) {
            if (processSlots[i].input.isEmpty()) {
                if (tryFillProcessSlot(i)) changed = true;
            }
        }

        if (changed) {
            markDirty();
        }
    }

    /** 从输入栈中取一个有效原料放入指定处理槽 */
    private boolean tryFillProcessSlot(int slotIndex) {
        ProcessSlot slot = processSlots[slotIndex];
        if (!slot.input.isEmpty()) return false;

        for (int i = 0; i < inputHandler.getSlots(); i++) {
            ItemStack stack = inputHandler.getStackInSlot(i);
            if (!stack.isEmpty() && getOutputForInput(stack) != null) {
                ItemStack taken = inputHandler.extractItem(i, 1, false);
                if (!taken.isEmpty()) {
                    slot.input = taken.copy();
                    slot.progress = 0;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据原料返回成品 (改进后能够识别所有原木)
     * 石头 (ore:stone)       → 活石
     * 原木 (ore:logWood)     → 活木
     */
    @Nullable
    private ItemStack getOutputForInput(ItemStack input) {
        // 1. 检查是否是原木 (logWood) —— 这是修复橡木无法转换的要点
        for (int oreId : OreDictionary.getOreIDs(input)) {
            String oreName = OreDictionary.getOreName(oreId);
            if ("logWood".equals(oreName)) {
                return getBotaniaItem("livingwood");
            }
        }

        // 2. 检查是否是石头 (stone)
        if (isStone(input)) {
            return getBotaniaItem("livingrock");
        }

        return null;
    }

    /** 检查物品是否为任何类型的原木 (增强兼容性) */
    private boolean isLogWood(ItemStack stack) {
        for (int id : OreDictionary.getOreIDs(stack)) {
            if ("logWood".equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }

    /** 检查物品是否为石头或圆石 (增强兼容性) */
    private boolean isStone(ItemStack stack) {
        for (int id : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(id);
            if ("stone".equals(name) || "cobblestone".equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** 安全地从 Botania 模组获取物品 */
    @Nullable
    private ItemStack getBotaniaItem(String itemName) {
        Item item = Item.getByNameOrId("botania:" + itemName);
        return item != null ? new ItemStack(item) : null;
    }

    /** 尝试将成品放入输出槽 */
    private boolean tryInsertOutput(ItemStack output) {
        if (output.isEmpty()) return false;
        ItemStack remaining = output.copy();

        // 先尝试合并到已有同类物品的槽
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            ItemStack existing = outputHandler.getStackInSlot(i);
            if (!existing.isEmpty() && existing.isItemEqual(output) && ItemStack.areItemStackTagsEqual(existing, output)) {
                int canMerge = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (canMerge > 0) {
                    existing.grow(canMerge);
                    remaining.shrink(canMerge);
                    outputHandler.setStackInSlot(i, existing);
                    if (remaining.isEmpty()) return true;
                }
            }
        }
        // 再尝试放入空槽
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            if (outputHandler.getStackInSlot(i).isEmpty()) {
                outputHandler.setStackInSlot(i, remaining.copy());
                return true;
            }
        }
        return false;
    }

    /** 从矿物词典判断是否为石头或原木 */
    private String getValidOreName(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (int id : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(id);
            if ("stone".equals(name)) return "stone";
            if ("logWood".equals(name)) return "logWood";
        }
        return null;
    }


    // ==================== 面限制（I/O方向） ====================
    private final IItemHandler inputWrapper = new IItemHandler() {
        @Override public int getSlots() { return INPUT_SLOTS; }
        @Nonnull @Override public ItemStack getStackInSlot(int slot) { return inputHandler.getStackInSlot(slot); }
        @Nonnull @Override public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) { return inputHandler.insertItem(slot, stack, simulate); }
        @Nonnull @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; } // 不允许从输入槽提取
        @Override public int getSlotLimit(int slot) { return inputHandler.getSlotLimit(slot); }
    };

    private final IItemHandler outputWrapper = new IItemHandler() {
        @Override public int getSlots() { return OUTPUT_SLOTS; }
        @Nonnull @Override public ItemStack getStackInSlot(int slot) { return outputHandler.getStackInSlot(slot); }
        @Nonnull @Override public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) { return stack; } // 不允许外部插入输出槽
        @Nonnull @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return outputHandler.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return outputHandler.getSlotLimit(slot); }
    };

    // GUI用：同时暴露输入+输出槽
    private final IItemHandler fullHandler = new IItemHandler() {
        @Override public int getSlots() { return INPUT_SLOTS + OUTPUT_SLOTS; }
        @Nonnull @Override public ItemStack getStackInSlot(int slot) {
            if (slot < INPUT_SLOTS) return inputHandler.getStackInSlot(slot);
            else return outputHandler.getStackInSlot(slot - INPUT_SLOTS);
        }
        @Nonnull @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot < INPUT_SLOTS) return inputHandler.insertItem(slot, stack, simulate);
            else return stack; // 不允许插入输出槽
        }
        @Nonnull @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < INPUT_SLOTS) return ItemStack.EMPTY; // 不允许从输入槽提取
            else return outputHandler.extractItem(slot - INPUT_SLOTS, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) {
            if (slot < INPUT_SLOTS) return inputHandler.getSlotLimit(slot);
            else return outputHandler.getSlotLimit(slot - INPUT_SLOTS);
        }
    };

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        if (capability == CapabilityEnergy.ENERGY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == null) return (T) fullHandler;          // GUI / 内部访问
            if (facing == EnumFacing.DOWN) return (T) outputWrapper;   // 底部只能抽取成品
            else return (T) inputWrapper;                              // 其他面只能输入原料
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) energyStorage;
        }
        return super.getCapability(capability, facing);
    }

    // ==================== NBT 持久化 ====================
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inputHandler.deserializeNBT(compound.getCompoundTag("InputInventory"));
        outputHandler.deserializeNBT(compound.getCompoundTag("OutputInventory"));
        energyStorage.readFromNBT(compound);   // 直接传入整个 compound，内部读取 "Energy"
        NBTTagList processList = compound.getTagList("ProcessSlots", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < processList.tagCount() && i < PROCESS_SLOTS; i++) {
            processSlots[i].deserialize(processList.getCompoundTagAt(i));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("InputInventory", inputHandler.serializeNBT());
        compound.setTag("OutputInventory", outputHandler.serializeNBT());
        energyStorage.writeToNBT(compound);
        NBTTagList processList = new NBTTagList();
        for (ProcessSlot slot : processSlots) {
            processList.appendTag(slot.serialize());
        }
        compound.setTag("ProcessSlots", processList);
        return compound;
    }

    // 以下方法供外部查询进度（可选）
    public int getProgress(int slot) {
        if (slot >= 0 && slot < PROCESS_SLOTS) return processSlots[slot].progress;
        return 0;
    }
    public int getMaxProgress() { return PROCESS_TIME; }
    public ItemStack getProcessingInput(int slot) {
        if (slot >= 0 && slot < PROCESS_SLOTS) return processSlots[slot].input;
        return ItemStack.EMPTY;
    }
}