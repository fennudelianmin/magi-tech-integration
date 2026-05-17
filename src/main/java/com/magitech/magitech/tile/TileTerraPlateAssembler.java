package com.magitech.magitech.tile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
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
    private static final int TICKS_PER_CRAFT = 120;

    // 槽位索引
    private static final int INPUT_SLOTS = 3; // 三个输入槽，可任意混合
    private static final int SLOT_OUTPUT = 3; // 输出槽固定第4个

    // 物品与元数据常量
    private static Item MANA_RESOURCE = null;
    private static final int META_MANASTEEL = 0;
    private static final int META_MANAPEARL = 1;
    private static final int META_MANADIAMOND = 2;
    private static final int META_TERRASTEEL = 4;

    public enum State {
        IDLE,
        CRAFTING
    }

    protected int mana;
    protected ISparkEntity attachedSpark;
    private State state = State.IDLE;
    private int progress;
    private int manaConsumed;

    // 面限制包装器
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();

    public TileTerraPlateAssembler() {
        super(INPUT_SLOTS, 1, ENERGY_CAPACITY); // 3输入，1输出
        this.mana = 0;
    }

    private static void initItems() {
        if (MANA_RESOURCE == null) {
            MANA_RESOURCE = Item.REGISTRY.getObject(new ResourceLocation("botania", "manaresource"));
            if (MANA_RESOURCE == null) {
                System.err.println("[TileTerraPlateAssembler] CRITICAL: 'botania:manaresource' not found! Crafting disabled.");
            }
        }
    }

    @Override
    public void update() {
        if (world.isRemote) return;
        initItems();

        switch (state) {
            case IDLE:
                tryStartCrafting();
                break;
            case CRAFTING:
                doCrafting();
                break;
        }
    }

    // 统计所有输入槽中指定元数据的物品总数
    private int countMaterial(int meta) {
        int count = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (isMaterial(stack, meta)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // 从输入槽消耗指定元数据物品1个
    private boolean consumeOne(int meta) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (isMaterial(stack, meta)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    // 判断物品是否是指定元数据的魔力资源
    private boolean isMaterial(ItemStack stack, int meta) {
        return !stack.isEmpty() && stack.getItem() == MANA_RESOURCE && stack.getItemDamage() == meta;
    }

    private void tryStartCrafting() {
        if (MANA_RESOURCE == null) return;

        // 统计三种材料总数，各至少1个
        if (countMaterial(META_MANASTEEL) < 1) return;
        if (countMaterial(META_MANAPEARL) < 1) return;
        if (countMaterial(META_MANADIAMOND) < 1) return;

        // 检查魔力与能量
        if (mana < MANA_COST_PER_CRAFT) return;
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 检查输出槽
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        ItemStack product = new ItemStack(MANA_RESOURCE, 1, META_TERRASTEEL);
        if (!output.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(output, product) ||
                    output.getCount() + 1 > output.getMaxStackSize()) {
                return; // 输出满
            }
        }

        state = State.CRAFTING;
        progress = 0;
        manaConsumed = 0;
        markDirty();
    }

    private void doCrafting() {
        if (MANA_RESOURCE == null) {
            state = State.IDLE;
            return;
        }

        // 再次确认材料充足
        if (countMaterial(META_MANASTEEL) < 1 ||
                countMaterial(META_MANAPEARL) < 1 ||
                countMaterial(META_MANADIAMOND) < 1) {
            return; // 材料意外不足，暂停
        }

        // 检查输出空间
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        ItemStack product = new ItemStack(MANA_RESOURCE, 1, META_TERRASTEEL);
        if (!output.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(output, product) ||
                    output.getCount() + 1 > output.getMaxStackSize()) {
                return; // 输出满，暂停
            }
        }

        // 检查能量
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 计算魔力消耗
        int remainingMana = MANA_COST_PER_CRAFT - manaConsumed;
        int manaPerTick = Math.max(1, MANA_COST_PER_CRAFT / TICKS_PER_CRAFT);
        int manaToConsume = Math.min(manaPerTick, Math.min(remainingMana, mana));
        if (manaToConsume <= 0) return; // 魔力不足，暂停

        energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        mana -= manaToConsume;
        manaConsumed += manaToConsume;
        progress = (manaConsumed * 100) / MANA_COST_PER_CRAFT;
        markDirty();

        if (manaConsumed >= MANA_COST_PER_CRAFT) {
            // 消耗材料各1个
            consumeOne(META_MANASTEEL);
            consumeOne(META_MANAPEARL);
            consumeOne(META_MANADIAMOND);

            if (output.isEmpty()) {
                itemHandler.setStackInSlot(SLOT_OUTPUT, product.copy());
            } else {
                output.grow(1);
            }

            state = State.IDLE;
            progress = 0;
            manaConsumed = 0;
            markDirty();
        }
    }

    // ---------- 输入/输出处理器 (保持面限制) ----------
    private class InputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return INPUT_SLOTS;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot >= 0 && slot < INPUT_SLOTS) return itemHandler.getStackInSlot(slot);
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isValidInput(stack)) return stack;
            // 任意槽插入，尝试堆叠/插入空位
            return insertItemToInputs(stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // 禁止提取
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        private boolean isValidInput(ItemStack stack) {
            if (stack.getItem() != MANA_RESOURCE) return false;
            int meta = stack.getItemDamage();
            return meta == META_MANASTEEL || meta == META_MANAPEARL || meta == META_MANADIAMOND;
        }

        private ItemStack insertItemToInputs(ItemStack stack, boolean simulate) {
            ItemStack remaining = stack.copy();
            // 先尝试堆叠到已有相同物品的槽位
            for (int i = 0; i < INPUT_SLOTS; i++) {
                remaining = itemHandler.insertItem(i, remaining, true);
                if (remaining.isEmpty()) break;
            }
            if (!simulate && remaining.getCount() != stack.getCount()) {
                // 执行实际插入
                ItemStack toInsert = stack.copy();
                for (int i = 0; i < INPUT_SLOTS; i++) {
                    toInsert = itemHandler.insertItem(i, toInsert, false);
                    if (toInsert.isEmpty()) break;
                }
                return toInsert;
            }
            return remaining;
        }
    }

    private class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
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
            return stack; // 禁止插入
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
            return itemHandler.getSlotLimit(SLOT_OUTPUT);
        }
    }

    // ---------- Capability 面限制 ----------
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

    // ---------- Mana / Spark ----------
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

    // ---------- 客户端同步用 getter ----------
    public State getState() { return state; }
    public int getProgress() { return progress; }
    public int getMaxMana() { return MANA_CAPACITY; }

    // ---------- NBT 持久化 ----------
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