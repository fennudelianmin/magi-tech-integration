package com.magitech.magitech.tile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.mana.spark.ISparkEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileTerraPlateAssembler extends TileBase implements ISparkAttachable {

    public static final int ENERGY_CAPACITY = 80000;
    public static final int ENERGY_PER_TICK = 32;
    public static final int MANA_CAPACITY = 1000000;
    public static final int MANA_COST_PER_CRAFT = 500000;
    private static final int TICKS_PER_CRAFT = 120; // 大约6秒

    // 槽位索引
    private static final int SLOT_MANASTEEL = 0;
    private static final int SLOT_MANAPEARL = 1;
    private static final int SLOT_MANADIAMOND = 2;
    private static final int SLOT_OUTPUT = 3;
    private static final int TOTAL_SLOTS = 4;

    // 缓存物品实例
    private static Item MANASTEEL;
    private static Item MANAPEARL;
    private static Item MANADIAMOND;
    private static Item TERRASTEEL;

    // 状态枚举
    public enum State {
        IDLE,
        CRAFTING
    }

    protected int mana;
    protected ISparkEntity attachedSpark;
    private State state = State.IDLE;
    private int progress;          // 0-100 百分比
    private int manaConsumed;      // 已消耗魔力

    // 面限制的包装器
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();

    public TileTerraPlateAssembler() {
        super(3, 1, ENERGY_CAPACITY); // 3个输入槽，1个输出槽
        this.mana = 0;
        // 初始化物品引用
        if (MANASTEEL == null) {
            MANASTEEL = Item.getByNameOrId("botania:manasteel");
            MANAPEARL = Item.getByNameOrId("botania:manapearl");
            MANADIAMOND = Item.getByNameOrId("botania:manadiamond");
            TERRASTEEL = Item.getByNameOrId("botania:terrasteel");
        }
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        switch (state) {
            case IDLE:
                tryStartCrafting();
                break;
            case CRAFTING:
                doCrafting();
                break;
        }
    }

    private void tryStartCrafting() {
        // 检查产物是否已定义
        if (TERRASTEEL == null) return;

        // 检查中心材料：三种各至少1个
        ItemStack manasteel = itemHandler.getStackInSlot(SLOT_MANASTEEL);
        ItemStack manapearl = itemHandler.getStackInSlot(SLOT_MANAPEARL);
        ItemStack manadiamond = itemHandler.getStackInSlot(SLOT_MANADIAMOND);
        if (manasteel.isEmpty() || manapearl.isEmpty() || manadiamond.isEmpty()) return;
        if (manasteel.getCount() < 1 || manapearl.getCount() < 1 || manadiamond.getCount() < 1) return;

        // 检查魔力
        if (mana < MANA_COST_PER_CRAFT) return;
        // 检查能量
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;
        // 检查输出槽是否可放入产物
        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT);
        ItemStack product = new ItemStack(TERRASTEEL);
        if (!outputStack.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(outputStack, product)
                || outputStack.getCount() + 1 > outputStack.getMaxStackSize()) {
                return; // 输出满，不能开始
            }
        }

        state = State.CRAFTING;
        progress = 0;
        manaConsumed = 0;
        markDirty();
    }

    private void doCrafting() {
        if (TERRASTEEL == null) {
            state = State.IDLE;
            return;
        }

        // 检查材料是否依旧满足（防止意外，不过输入面只进不出，通常不会减少）
        ItemStack manasteel = itemHandler.getStackInSlot(SLOT_MANASTEEL);
        ItemStack manapearl = itemHandler.getStackInSlot(SLOT_MANAPEARL);
        ItemStack manadiamond = itemHandler.getStackInSlot(SLOT_MANADIAMOND);
        if (manasteel.getCount() < 1 || manapearl.getCount() < 1 || manadiamond.getCount() < 1) {
            // 材料不足，暂停（实际不会发生）
            return;
        }

        // 检查输出槽是否可接收产物（若满则暂停）
        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT);
        ItemStack product = new ItemStack(TERRASTEEL);
        if (!outputStack.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(outputStack, product)
                || outputStack.getCount() + 1 > outputStack.getMaxStackSize()) {
                // 输出满，暂停（不消耗能量/魔力，进度保持）
                return;
            }
        }

        // 检查能量
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 计算本 tick 可消耗的魔力
        int remainingMana = MANA_COST_PER_CRAFT - manaConsumed;
        int manaPerTick = Math.max(1, MANA_COST_PER_CRAFT / TICKS_PER_CRAFT);
        int manaToConsume = Math.min(manaPerTick, Math.min(remainingMana, mana));
        if (manaToConsume <= 0) return; // 无魔力可用，暂停

        // 消耗能量
        energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        // 消耗魔力
        mana -= manaToConsume;
        manaConsumed += manaToConsume;
        progress = (manaConsumed * 100) / MANA_COST_PER_CRAFT;

        markDirty();

        // 检查是否完成
        if (manaConsumed >= MANA_COST_PER_CRAFT) {
            // 完成合成：消耗材料各1个
            manasteel.shrink(1);
            manapearl.shrink(1);
            manadiamond.shrink(1);

            // 放入产物
            if (outputStack.isEmpty()) {
                itemHandler.setStackInSlot(SLOT_OUTPUT, product.copy());
            } else {
                outputStack.grow(1);
            }

            // 重置状态，准备下一轮
            state = State.IDLE;
            progress = 0;
            manaConsumed = 0;
            markDirty();
        }
    }

    // --- 面限制的 ItemHandler 实现 ---

    private class InputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 3; // 只暴露输入槽
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot >= 0 && slot < 3) return itemHandler.getStackInSlot(slot);
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= 3) return stack;
            // 检查是否是允许的材料
            if (!isValidInput(stack)) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 不允许提取
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot >= 0 && slot < 3) return itemHandler.getSlotLimit(slot);
            return 0;
        }

        private boolean isValidInput(ItemStack stack) {
            if (stack.isEmpty()) return false;
            Item item = stack.getItem();
            return item == MANASTEEL || item == MANAPEARL || item == MANADIAMOND;
        }
    }

    private class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1; // 只暴露输出槽
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot == 0) return itemHandler.getStackInSlot(SLOT_OUTPUT);
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            // 不允许插入
            return stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0) {
                return itemHandler.extractItem(SLOT_OUTPUT, amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 0) return itemHandler.getSlotLimit(SLOT_OUTPUT);
            return 0;
        }
    }

    // --- Capability 覆写，面限制 ---
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
            if (facing == null) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
            if (facing == EnumFacing.DOWN) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(outputHandler);
            } else {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inputHandler);
            }
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        T customCap = getCustomCapability(capability, facing);
        if (customCap != null) return customCap;
        return super.getCapability(capability, facing);
    }

    // --- IManaBlock & IManaReceiver ---
    @Override
    public int getCurrentMana() { return mana; }

    @Override
    public boolean isFull() { return mana >= MANA_CAPACITY; }

    @Override
    public void recieveMana(int mana) {
        this.mana = Math.min(MANA_CAPACITY, this.mana + mana);
        markDirty();
    }

    @Override
    public boolean canRecieveManaFromBursts() { return !isFull(); }

    // --- ISparkAttachable ---
    @Override
    public boolean canAttachSpark(ItemStack stack) { return attachedSpark == null; }

    @Override
    public void attachSpark(ISparkEntity entity) { this.attachedSpark = entity; }

    @Override
    public ISparkEntity getAttachedSpark() { return attachedSpark; }

    @Override
    public boolean areIncomingTranfersDone() { return isFull(); }

    @Override
    public int getAvailableSpaceForMana() { return Math.max(0, MANA_CAPACITY - mana); }

    // --- 状态 getter ---
    public State getState() { return state; }
    public int getProgress() { return progress; }
    public int getMaxMana() { return MANA_CAPACITY; }

    // --- NBT 读写 ---
    @Override
    protected void readCustomNBT(NBTTagCompound compound) {
        mana = compound.getInteger("mana");
        state = State.values()[compound.getInteger("state")];
        progress = compound.getInteger("progress");
        manaConsumed = compound.getInteger("manaConsumed");
    }

    @Override
    protected void writeCustomNBT(NBTTagCompound compound) {
        compound.setInteger("mana", mana);
        compound.setInteger("state", state.ordinal());
        compound.setInteger("progress", progress);
        compound.setInteger("manaConsumed", manaConsumed);
    }
}