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

/**
 * 泰拉锭自动组装机。
 * <p>
 * 自动将魔力钢、魔力珍珠、魔力钻石合成为泰拉锭。
 * 需要从火花网络供给大量魔力（50万mana/次），同时消耗 FE 能量推进。
 * 合成过程持续约 6 秒（120 ticks），每秒消耗等量魔力。
 * <p>
 * 侧面控制：
 *   下方 → 只可抽取（取出泰拉锭）
 *   其他面 → 只可插入（放入三种原料）
 */
public class TileTerraPlateAssembler extends TileBase implements ISparkAttachable {

    /** 最大能量容量（FE） */
    public static final int ENERGY_CAPACITY = 80000;
    /** 每 tick 消耗的能量（FE） */
    public static final int ENERGY_PER_TICK = 32;
    /** 最大魔力容量 */
    public static final int MANA_CAPACITY = 1000000;
    /** 单次合成所需魔力总量 */
    public static final int MANA_COST_PER_CRAFT = 500000;
    /** 单次合成所需 tick 数（约 6 秒） */
    private static final int TICKS_PER_CRAFT = 120;

    // 槽位索引
    /** 输入槽数量（3 个原料槽，可任意混合摆放） */
    private static final int INPUT_SLOTS = 3;
    /** 输出槽索引（第 4 个槽，索引 3） */
    private static final int SLOT_OUTPUT = 3;

    // Botania 魔力资源物品和元数据常量
    private static Item MANA_RESOURCE = null;
    private static final int META_MANASTEEL = 0;    // 魔力钢
    private static final int META_MANAPEARL = 1;    // 魔力珍珠
    private static final int META_MANADIAMOND = 2;  // 魔力钻石
    private static final int META_TERRASTEEL = 4;   // 泰拉锭

    /**
     * 机器状态枚举。
     * IDLE — 空闲，检查原料是否齐全
     * CRAFTING — 合成中，消耗魔力推进进度
     */
    public enum State {
        IDLE,
        CRAFTING
    }

    /** 当前存储的魔力值 */
    protected int mana;
    /** 连接的火花实体 */
    protected ISparkEntity attachedSpark;
    private State state = State.IDLE;
    /** 合成进度百分比（0~100） */
    private int progress;
    /** 当前合成已消耗的魔力值 */
    private int manaConsumed;

    // 面限制包装器
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();

    public TileTerraPlateAssembler() {
        super(INPUT_SLOTS, 1, ENERGY_CAPACITY); // 3输入，1输出
        this.mana = 0;
    }

    /**
     * 延迟初始化 Botania 物品引用。
     * 因为构造阶段 Botania 物品可能尚未注册，所以在第一次 update 时查找。
     */
    private static void initItems() {
        if (MANA_RESOURCE == null) {
            MANA_RESOURCE = Item.REGISTRY.getObject(new ResourceLocation("botania", "manaresource"));
            if (MANA_RESOURCE == null) {
                System.err.println("[TileTerraPlateAssembler] CRITICAL: 'botania:manaresource' not found! Crafting disabled.");
            }
        }
    }

    /**
     * 每 tick 更新。仅在服务端执行。
     * IDLE → 检查输入原料是否齐全，尝试开始合成
     * CRAFTING → 消耗魔力和能量推进进度，完成后消耗原料并输出产物
     */
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

    /** 统计所有输入槽中指定元数据魔力的物品总数 */
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

    /** 从输入槽消耗一个指定元数据的物品，成功返回 true */
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

    /** 判断物品是否是指定元数据的 botania:manaresource */
    private boolean isMaterial(ItemStack stack, int meta) {
        return !stack.isEmpty() && stack.getItem() == MANA_RESOURCE && stack.getItemDamage() == meta;
    }

    /**
     * 尝试启动合成。
     * 条件：三种原料各至少1个、魔力≥50万、FE足够、输出槽有空位。
     */
    private void tryStartCrafting() {
        if (MANA_RESOURCE == null) return;

        // 检查三种原料是否齐全（各至少1个）
        if (countMaterial(META_MANASTEEL) < 1) return;
        if (countMaterial(META_MANAPEARL) < 1) return;
        if (countMaterial(META_MANADIAMOND) < 1) return;

        // 检查魔力与能量
        if (mana < MANA_COST_PER_CRAFT) return;
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 检查输出槽是否有空位或可堆叠
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        ItemStack product = new ItemStack(MANA_RESOURCE, 1, META_TERRASTEEL);
        if (!output.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(output, product) ||
                    output.getCount() + 1 > output.getMaxStackSize()) {
                return;
            }
        }

        state = State.CRAFTING;
        progress = 0;
        manaConsumed = 0;
        markDirty();
    }

    /**
     * 推进合成进度。
     * 每 tick 消耗 32 FE 和一定量魔力，按比例推进进度。
     * 当累计消耗魔力达到 50 万时，消耗三个原料各1个，输出1个泰拉锭。
     */
    private void doCrafting() {
        if (MANA_RESOURCE == null) {
            state = State.IDLE;
            return;
        }

        // 二次确认材料仍然充足
        if (countMaterial(META_MANASTEEL) < 1 ||
                countMaterial(META_MANAPEARL) < 1 ||
                countMaterial(META_MANADIAMOND) < 1) {
            return;
        }

        // 检查输出空间
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        ItemStack product = new ItemStack(MANA_RESOURCE, 1, META_TERRASTEEL);
        if (!output.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(output, product) ||
                    output.getCount() + 1 > output.getMaxStackSize()) {
                return;
            }
        }

        // 检查能量
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 计算每 tick 应消耗的魔力（均匀分配）
        int remainingMana = MANA_COST_PER_CRAFT - manaConsumed;
        int manaPerTick = Math.max(1, MANA_COST_PER_CRAFT / TICKS_PER_CRAFT);
        int manaToConsume = Math.min(manaPerTick, Math.min(remainingMana, mana));
        if (manaToConsume <= 0) return;

        energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        mana -= manaToConsume;
        manaConsumed += manaToConsume;
        progress = (manaConsumed * 100) / MANA_COST_PER_CRAFT;
        markDirty();

        if (manaConsumed >= MANA_COST_PER_CRAFT) {
            // 合成完成：消耗原料各1个
            consumeOne(META_MANASTEEL);
            consumeOne(META_MANAPEARL);
            consumeOne(META_MANADIAMOND);

            // 输出产物
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

    /** 输入处理器：允许插入三种原料，禁止抽取 */
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
            return insertItemToInputs(stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        /** 检查是否为合法的原料：必须是魔力钢/珍珠/钻石 */
        private boolean isValidInput(ItemStack stack) {
            if (stack.getItem() != MANA_RESOURCE) return false;
            int meta = stack.getItemDamage();
            return meta == META_MANASTEEL || meta == META_MANAPEARL || meta == META_MANADIAMOND;
        }

        /** 智能插入：先尝试堆叠到已有同种物品的槽，再放入空槽 */
        private ItemStack insertItemToInputs(ItemStack stack, boolean simulate) {
            ItemStack remaining = stack.copy();
            for (int i = 0; i < INPUT_SLOTS; i++) {
                remaining = itemHandler.insertItem(i, remaining, true);
                if (remaining.isEmpty()) break;
            }
            if (!simulate && remaining.getCount() != stack.getCount()) {
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

    /** 输出处理器：只暴露1个输出槽，只可抽取不可插入 */
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
