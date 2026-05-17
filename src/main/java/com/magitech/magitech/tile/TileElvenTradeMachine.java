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

public class TileElvenTradeMachine extends TileBase implements ISparkAttachable {

    public static final int ENERGY_CAPACITY = 20000;
    public static final int ENERGY_PER_TICK = 8;
    public static final int MANA_CAPACITY = 50000;
    public static final int DEFAULT_MANA_COST = 2000;

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    // ==================== 配方缓存 ====================
    private static List<CachedElvenRecipe> recipeCache = null;

    static {
        initRecipeCache();
    }

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
            if (invalid || merged.size() != 1) continue; // 仅支持单一类型输入

            // 获取代表物品和需求数量
            Map.Entry<ItemDefinition, Integer> entry = merged.entrySet().iterator().next();
            ItemStack inputStack = entry.getKey().exampleStack.copy();
            inputStack.setCount(entry.getValue());

            // 获取输出（取第一个产物）
            List<ItemStack> outputs = recipe.getOutputs();
            if (outputs.isEmpty()) continue;
            ItemStack output = outputs.get(0).copy();

            CachedElvenRecipe cr = new CachedElvenRecipe();
            cr.input = inputStack;
            cr.output = output;
            cr.manaCost = DEFAULT_MANA_COST; // 可在此处通过配置覆盖
            cr.originalRecipe = recipe;
            recipeCache.add(cr);
        }
    }

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

    // 内部缓存类
    static class CachedElvenRecipe {
        ItemStack input;          // 合并后的输入（数量即需求数）
        ItemStack output;         // 产物
        int manaCost;
        RecipeElvenTrade originalRecipe;
    }

    // 物品定义（支持矿物词典比较）
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
    protected int mana;
    protected ISparkEntity attachedSpark;
    private State state = State.IDLE;
    private int progress;
    private int manaCost;
    private CachedElvenRecipe currentCachedRecipe;   // 当前匹配的缓存配方
    private ItemStack pendingOutput;                  // 预先生成的产物（保留用于输出）

    public enum State {
        IDLE,          // 等待原料
        TRADING,       // 交易中
        OUTPUT         // 产物输出
    }

    // 侧面能力代理
    private final IItemHandler inputHandler = new InputHandler();
    private final IItemHandler outputHandler = new OutputHandler();

    public TileElvenTradeMachine() {
        super(1, 1, ENERGY_CAPACITY);
        this.mana = 0;
        this.manaCost = DEFAULT_MANA_COST;
    }

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

    private void tryStartTrading() {
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        ItemStack inputSlot = itemHandler.getStackInSlot(INPUT_SLOT);
        if (inputSlot.isEmpty()) return;

        // 检查魔力
        if (mana < DEFAULT_MANA_COST) return;

        // 遍历缓存匹配配方
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

    private void doTrading() {
        if (!consumeEnergy()) return;

        int manaPerTick = Math.max(1, manaCost / 20); // 约1秒完成
        if (mana < manaPerTick) return;

        mana -= manaPerTick;
        progress += manaPerTick;

        if (progress >= manaCost) {
            // 消耗输入物品（扣除配方所需数量）
            itemHandler.getStackInSlot(INPUT_SLOT).shrink(currentCachedRecipe.input.getCount());
            state = State.OUTPUT;
        }
        markDirty();
    }

    private void doOutput() {
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, pendingOutput.copy());
        } else {
            outputSlot.grow(pendingOutput.getCount());
        }
        resetState();
    }

    private boolean consumeEnergy() {
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return false;
        energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        return true;
    }

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

    // 输入代理：仅暴露输入槽，禁止提取
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

    // 输出代理：仅暴露输出槽，禁止插入
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