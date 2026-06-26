package com.magitech.magitech.tile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.mana.spark.ISparkEntity;
import vazkii.botania.api.recipe.RecipeElvenTrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * 精灵交易自动化机器。
 * <p>
 * 自动执行 Botania 精灵交易。将 Botania 原版精灵交易配方缓存到内存中，
 * 仅支持单一类型输入的配方（例如 1 个梦境草地毯 → 1 个精灵草地毯）。
 * 需要消耗魔力和 FE 能量，约 1 秒完成一次交易。
 * <p>
 * 工作流程：
 *   IDLE → 检查输入和配方匹配 → TRADING
 *   TRADING → 消耗魔力推进进度 → 消耗输入物品 → OUTPUT
 *   OUTPUT → 写入输出槽 → IDLE
 * <p>
 * 侧面控制：
 *   下方 → 只可抽取（取出交易产物）
 *   其他面 → 只可插入（放入交易原料）
 */
public class TileElvenTradeMachine extends TileBase implements ISparkAttachable {

    /** 最大能量容量（FE） */
    public static final int ENERGY_CAPACITY = 20000;
    /** 每 tick 消耗的能量（FE） */
    public static final int ENERGY_PER_TICK = 8;
    /** 最大魔力容量 */
    public static final int MANA_CAPACITY = 50000;
    /** 默认每次交易消耗的魔力值 */
    public static final int DEFAULT_MANA_COST = 2000;

    /** 输入槽索引 */
    private static final int INPUT_SLOT = 0;
    /** 输出槽索引 */
    private static final int OUTPUT_SLOT = 1;

    // ==================== 配方缓存 ====================

    /** 静态配方缓存，启动时从 BotaniaAPI 加载所有精灵交易配方 */
    private static List<CachedElvenRecipe> recipeCache = null;

    static {
        initRecipeCache();
    }

    /**
     * 初始化配方缓存。
     * 遍历 Botania 的所有精灵交易配方，将输入物品按物品定义合并后，
     * 仅保留那些只有单一类型输入的配方（合并后 merged.size() == 1）。
     * 多类型输入的复杂配方不予支持。
     */
    private static void initRecipeCache() {
        recipeCache = new ArrayList<>();
        for (RecipeElvenTrade recipe : BotaniaAPI.elvenTradeRecipes) {
            // 解析并合并输入，只保留合并后为单一类型的配方
            Map<ItemDefinition, Integer> merged = new HashMap<>();
            boolean invalid = false;
            for (Object input : recipe.getInputs()) {
                ItemStack stack = parseInputObject(input);
                if (stack.isEmpty()) {
                    invalid = true;
                    break;
                }
                ItemDefinition def = new ItemDefinition(stack);
                merged.put(def, merged.getOrDefault(def, 0) + stack.getCount());
            }
            if (invalid || merged.size() != 1) continue;

            // 获取代表物品和需求数量
            Map.Entry<ItemDefinition, Integer> entry = merged.entrySet().iterator().next();
            ItemStack inputStack = entry.getKey().exampleStack.copy();
            inputStack.setCount(entry.getValue());

            // 取第一个产物作为输出
            List<ItemStack> outputs = recipe.getOutputs();
            if (outputs.isEmpty()) continue;
            ItemStack output = outputs.get(0).copy();

            CachedElvenRecipe cr = new CachedElvenRecipe();
            cr.input = inputStack;
            cr.output = output;
            cr.manaCost = DEFAULT_MANA_COST;
            cr.originalRecipe = recipe;
            recipeCache.add(cr);
        }
    }

    /** 将配方输入对象（ItemStack 或矿物词典字符串）解析为 ItemStack */
    private static ItemStack parseInputObject(Object input) {
        if (input instanceof ItemStack) {
            return ((ItemStack) input).copy();
        } else if (input instanceof String) {
            List<ItemStack> ores = OreDictionary.getOres((String) input);
            if (!ores.isEmpty()) {
                return ores.get(0).copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /** 缓存配方结构体：记录输入需求、产物、魔力消耗和原始配方引用 */
    static class CachedElvenRecipe {
        /** 合并后的输入（数量即需求数） */
        ItemStack input;
        /** 产物 */
        ItemStack output;
        /** 所需魔力 */
        int manaCost;
        /** 原始 Botania 配方引用 */
        RecipeElvenTrade originalRecipe;
    }

    /**
     * 物品定义，用于配方匹配中的物品比较。
     * 支持精确匹配和矿物词典兼容匹配。
     */
    static class ItemDefinition {
        ItemStack exampleStack;
        int[] oreIDs;

        ItemDefinition(ItemStack stack) {
            this.exampleStack = stack.copy();
            this.exampleStack.setCount(1);
            this.oreIDs = OreDictionary.getOreIDs(stack);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemDefinition)) return false;
            ItemDefinition that = (ItemDefinition) o;
            if (ItemStack.areItemsEqual(this.exampleStack, that.exampleStack)) return true;
            if (this.oreIDs.length > 0 && that.oreIDs.length > 0) {
                for (int id1 : this.oreIDs) {
                    for (int id2 : that.oreIDs) {
                        if (id1 == id2) return true;
                    }
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Item.getIdFromItem(exampleStack.getItem()) * 32768 + exampleStack.getItemDamage();
        }
    }

    // ==================== 机器字段 ====================

    /** 当前存储的魔力值 */
    protected int mana;
    /** 连接的火花实体 */
    protected ISparkEntity attachedSpark;
    private State state = State.IDLE;
    /** 当前交易进度累计消耗的魔力值 */
    private int progress;
    /** 当前交易所需魔力值 */
    private int manaCost;
    /** 当前匹配到的缓存配方 */
    private CachedElvenRecipe currentCachedRecipe;
    /** 预先生成的产物（保留用于输出阶段） */
    private ItemStack pendingOutput;

    /**
     * 机器状态枚举。
     * IDLE    — 等待原料并尝试匹配配方
     * TRADING — 交易中，消耗魔力推进进度
     * OUTPUT  — 交易完成，将产物写入输出槽
     */
    public enum State {
        IDLE,
        TRADING,
        OUTPUT
    }

    // 侧面能力代理
    private final IItemHandler inputHandler = new InputHandler();
    private final IItemHandler outputHandler = new OutputHandler();

    public TileElvenTradeMachine() {
        super(1, 1, ENERGY_CAPACITY);
        this.mana = 0;
        this.manaCost = DEFAULT_MANA_COST;
    }

    /**
     * 每 tick 更新。仅在服务端执行。
     * 状态机与符文祭坛机器类似：
     * IDLE → TRADING → OUTPUT → IDLE
     */
    @Override
    public void update() {
        if (world.isRemote) return;

        switch (state) {
            case IDLE:
                tryStartTrading();
                break;
            case TRADING:
                doTrading();
                break;
            case OUTPUT:
                doOutput();
                break;
        }
    }

    /**
     * 尝试启动一次交易。
     * 检查：FE 能量、输入物品、魔力、配方匹配、输出空间。
     * 匹配成功后进入 TRADING 状态。
     */
    private void tryStartTrading() {
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        ItemStack inputSlot = itemHandler.getStackInSlot(INPUT_SLOT);
        if (inputSlot.isEmpty()) return;

        // 检查魔力
        if (mana < DEFAULT_MANA_COST) return;

        // 遍历配方缓存匹配
        for (CachedElvenRecipe cr : recipeCache) {
            ItemStack required = cr.input;

            // 检查输入物品是否匹配且数量足够
            if (!ItemHandlerHelper.canItemStacksStack(inputSlot, required)
                    || inputSlot.getCount() < required.getCount()) {
                continue;
            }

            // 检查输出槽空间
            ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (!outputSlot.isEmpty()) {
                if (!ItemHandlerHelper.canItemStacksStack(outputSlot, cr.output)
                        || outputSlot.getCount() + cr.output.getCount() > outputSlot.getMaxStackSize()) {
                    continue;
                }
            }

            // 匹配成功
            currentCachedRecipe = cr;
            manaCost = cr.manaCost;
            pendingOutput = cr.output.copy();
            break;
        }

        if (currentCachedRecipe == null || pendingOutput == null) {
            resetState();
            return;
        }

        state = State.TRADING;
        progress = 0;
    }

    /**
     * 推进交易进度。
     * 每 tick 消耗 FE 和一定魔力，约 20 tick 完成一次交易。
     * 进度达到所需魔力后，从输入槽扣除原料，进入 OUTPUT 状态。
     */
    private void doTrading() {
        if (!consumeEnergy()) return;

        int manaPerTick = Math.max(1, manaCost / 20);
        if (mana < manaPerTick) return;

        mana -= manaPerTick;
        progress += manaPerTick;

        if (progress >= manaCost) {
            // 消耗输入物品
            itemHandler.getStackInSlot(INPUT_SLOT).shrink(currentCachedRecipe.input.getCount());
            state = State.OUTPUT;
        }
        markDirty();
    }

    /** 将预生成的产物写入输出槽 */
    private void doOutput() {
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, pendingOutput.copy());
        } else {
            outputSlot.grow(pendingOutput.getCount());
        }
        resetState();
    }

    /** 每 tick 消耗 FE 能量 */
    private boolean consumeEnergy() {
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return false;
        energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        return true;
    }

    /** 重置机器状态为 IDLE */
    private void resetState() {
        state = State.IDLE;
        progress = 0;
        manaCost = DEFAULT_MANA_COST;
        currentCachedRecipe = null;
        pendingOutput = null;
        markDirty();
    }

    // ==================== 魔力与火花接口 ====================

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

    // ==================== 状态 Getters ====================

    public State getState() { return state; }
    public int getProgress() { return progress; }
    public int getManaCost() { return manaCost; }
    public int getMaxMana() { return MANA_CAPACITY; }

    // ==================== NBT 持久化 ====================

    @Override
    protected void readCustomNBT(NBTTagCompound compound) {
        mana = compound.getInteger("mana");
        state = State.values()[compound.getInteger("state")];
        progress = compound.getInteger("progress");
        manaCost = compound.getInteger("manaCost");
    }

    @Override
    protected void writeCustomNBT(NBTTagCompound compound) {
        compound.setInteger("mana", mana);
        compound.setInteger("state", state.ordinal());
        compound.setInteger("progress", progress);
        compound.setInteger("manaCost", manaCost);
    }

    // ==================== 侧面自动化控制 ====================

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == EnumFacing.DOWN) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(outputHandler);
            } else {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inputHandler);
            }
        }
        return super.getCapability(capability, facing);
    }

    /** 输入代理：暴露输入槽，只允许插入，禁止抽取 */
    private class InputHandler implements IItemHandler {
        @Override
        public int getSlots() { return 1; }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0) return ItemStack.EMPTY;
            return itemHandler.getStackInSlot(INPUT_SLOT);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot != 0) return stack;
            return itemHandler.insertItem(INPUT_SLOT, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot != 0) return 0;
            return itemHandler.getSlotLimit(INPUT_SLOT);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot != 0) return false;
            return itemHandler.isItemValid(INPUT_SLOT, stack);
        }
    }

    /** 输出代理：暴露输出槽，只允许抽取，禁止插入 */
    private class OutputHandler implements IItemHandler {
        @Override
        public int getSlots() { return 1; }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0) return ItemStack.EMPTY;
            return itemHandler.getStackInSlot(OUTPUT_SLOT);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0) return ItemStack.EMPTY;
            return itemHandler.extractItem(OUTPUT_SLOT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot != 0) return 0;
            return itemHandler.getSlotLimit(OUTPUT_SLOT);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return false;
        }
    }
}
