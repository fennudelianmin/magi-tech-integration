package com.magitech.magitech.tile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.mana.spark.ISparkEntity;
import vazkii.botania.api.recipe.RecipeRuneAltar;

import java.util.*;

public class TileRuneAltarMachine extends TileBase implements ISparkAttachable {

    public static final int ENERGY_CAPACITY = 40000;
    public static final int ENERGY_PER_TICK = 16;
    public static final int MANA_CAPACITY = 200000;

    private static final int INPUT_SLOTS = 32;
    private static final int OUTPUT_SLOTS = 32;
    private static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    private static final int FIRST_OUTPUT_SLOT = INPUT_SLOTS;

    public enum State {
        IDLE,
        CRAFTING,
        OUTPUT
    }

    // ==================== 配方缓存 ====================
    private static List<CachedRecipe> recipeCache = null;

    static {
        initRecipeCache();
    }

    private static void initRecipeCache() {
        recipeCache = new ArrayList<>();
        for (RecipeRuneAltar recipe : BotaniaAPI.runeAltarRecipes) {
            CachedRecipe cr = new CachedRecipe();
            cr.manaUsage = recipe.getManaUsage();
            cr.output = recipe.getOutput().copy();
            cr.originalRecipe = recipe;

            // 解析配方输入，并合并同类物品
            Map<ItemDefinition, Integer> merged = new HashMap<>();
            for (Object input : recipe.getInputs()) {
                ItemStack stack = parseInputObject(input);
                if (stack.isEmpty()) continue;
                ItemDefinition def = new ItemDefinition(stack);
                merged.put(def, merged.getOrDefault(def, 0) + stack.getCount());
            }

            // 区分催化剂和非催化剂
            for (Map.Entry<ItemDefinition, Integer> entry : merged.entrySet()) {
                ItemStack representative = entry.getKey().exampleStack;
                int count = entry.getValue();

                boolean isCatalyst = false;
                for (int id : OreDictionary.getOreIDs(representative)) {
                    if (OreDictionary.getOreName(id).startsWith("rune")) {
                        isCatalyst = true;
                        break;
                    }
                }

                ItemStack stack = representative.copy();
                stack.setCount(count);
                if (isCatalyst) {
                    cr.catalysts.add(stack);
                } else {
                    cr.nonCatalysts.add(stack);
                }
            }

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

    static class CachedRecipe {
        List<ItemStack> nonCatalysts = new ArrayList<>();   // 合并后的普通原料
        List<ItemStack> catalysts = new ArrayList<>();      // 合并后的催化剂（符文）
        ItemStack output;
        int manaUsage;
        RecipeRuneAltar originalRecipe; // 保留原始配方，用于输出等
    }

    static class ItemDefinition {
        ItemStack exampleStack; // 用于展示，count=1
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
            // 先精确匹配物品
            if (ItemStack.areItemsEqual(this.exampleStack, that.exampleStack)) return true;
            // 再检查矿物词典是否有交集
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
            // 用物品基础哈希，矿物词典匹配时哈希可能碰撞，但HashMap能正确处理
            return Item.getIdFromItem(exampleStack.getItem()) * 32768 + exampleStack.getItemDamage();
        }
    }

    // ==================== 机器字段 ====================
    protected int mana;
    protected ISparkEntity attachedSpark;
    private State state = State.IDLE;
    private int progress;
    private RecipeRuneAltar currentRecipe;
    private List<ItemStack> catalysts;
    private int manaCost;

    public TileRuneAltarMachine() {
        super(INPUT_SLOTS, OUTPUT_SLOTS, ENERGY_CAPACITY);
        this.mana = 0;
        this.progress = 0;
        this.catalysts = new ArrayList<>();
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
            case OUTPUT:
                doOutput();
                break;
        }
    }

    private void tryStartCrafting() {
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 收集并合并输入槽物品
        List<ItemStack> mergedInputs = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ItemStack copy = stack.copy();
            boolean merged = false;
            for (ItemStack existing : mergedInputs) {
                if (ItemHandlerHelper.canItemStacksStack(existing, copy)) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    int toAdd = Math.min(copy.getCount(), space);
                    existing.grow(toAdd);
                    copy.shrink(toAdd);
                    if (copy.isEmpty()) {
                        merged = true;
                        break;
                    }
                }
            }
            if (!merged && !copy.isEmpty()) {
                mergedInputs.add(copy);
            }
        }
        if (mergedInputs.isEmpty()) return;

        // 构建输入Map
        Map<ItemDefinition, Integer> inputMap = new HashMap<>();
        for (ItemStack stack : mergedInputs) {
            ItemDefinition def = new ItemDefinition(stack);
            inputMap.put(def, inputMap.getOrDefault(def, 0) + stack.getCount());
        }

        // 与缓存配方匹配
        currentRecipe = null;
        catalysts.clear();
        CachedRecipe matchedCache = null;

        for (CachedRecipe cr : recipeCache) {
            // 检查魔力
            if (mana < cr.manaUsage) continue;

            Map<ItemDefinition, Integer> remaining = new HashMap<>(inputMap);

            // 检查非催化剂数量是否足够
            boolean enough = true;
            for (ItemStack need : cr.nonCatalysts) {
                ItemDefinition def = new ItemDefinition(need);
                int have = remaining.getOrDefault(def, 0);
                if (have < need.getCount()) {
                    enough = false;
                    break;
                }
                remaining.put(def, have - need.getCount());
            }
            if (!enough) continue;

            // 检查催化剂数量是否足够
            for (ItemStack cat : cr.catalysts) {
                ItemDefinition def = new ItemDefinition(cat);
                int have = remaining.getOrDefault(def, 0);
                if (have < cat.getCount()) {
                    enough = false;
                    break;
                }
                remaining.put(def, have - cat.getCount());
            }
            if (!enough) continue;

            // 扣除后，剩余物品只能是催化剂（允许玩家多放符文）
            boolean hasInvalid = false;
            for (Map.Entry<ItemDefinition, Integer> entry : remaining.entrySet()) {
                if (entry.getValue() <= 0) continue;
                boolean isCatalyst = false;
                for (int id : OreDictionary.getOreIDs(entry.getKey().exampleStack)) {
                    if (OreDictionary.getOreName(id).startsWith("rune")) {
                        isCatalyst = true;
                        break;
                    }
                }
                if (!isCatalyst) {
                    hasInvalid = true;
                    break;
                }
            }
            if (hasInvalid) continue;

            // 匹配成功
            currentRecipe = cr.originalRecipe;
            manaCost = cr.manaUsage;
            matchedCache = cr;
            break;
        }

        if (currentRecipe == null) return;
        System.out.println("匹配到的配方：" + currentRecipe);

        // 记录催化剂列表（从缓存复制，用于输出时移动）
        catalysts.clear();
        for (ItemStack cat : matchedCache.catalysts) {
            catalysts.add(cat.copy());
        }

        // 检查输出空间
        if (!canFitOutputs(currentRecipe.getOutput(), catalysts)) {
            currentRecipe = null;
            return;
        }

        state = State.CRAFTING;
        progress = 0;
    }

    private boolean canFitOutputs(ItemStack output, List<ItemStack> catalysts) {
        ItemStackHandler tempOutput = new ItemStackHandler(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            tempOutput.setStackInSlot(i, itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT + i).copy());
        }
        ItemStack remaining = ItemHandlerHelper.insertItemStacked(tempOutput, output.copy(), true);
        if (!remaining.isEmpty()) return false;
        for (ItemStack cat : catalysts) {
            remaining = ItemHandlerHelper.insertItemStacked(tempOutput, cat.copy(), true);
            if (!remaining.isEmpty()) return false;
        }
        return true;
    }

    private void doCrafting() {
        if (!consumeEnergy()) return;
        int manaPerTick = Math.max(1, manaCost / 60);
        if (mana < manaPerTick) return;

        mana -= manaPerTick;
        progress += manaPerTick;

        if (progress >= manaCost) {
            // 消耗所有非催化剂（此时输入中非催化剂数量必定与配方完全一致，直接清空即可）
            for (int i = 0; i < INPUT_SLOTS; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                boolean isCatalyst = false;
                for (int id : OreDictionary.getOreIDs(stack)) {
                    if (OreDictionary.getOreName(id).startsWith("rune")) {
                        isCatalyst = true;
                        break;
                    }
                }
                if (!isCatalyst) {
                    itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
            state = State.OUTPUT;
        }
        markDirty();
    }

    private void doOutput() {
        ItemStackHandler outputView = new ItemStackHandler(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            outputView.setStackInSlot(i, itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT + i));
        }

        ItemStack result = ItemHandlerHelper.insertItemStacked(outputView, currentRecipe.getOutput().copy(), false);
        if (!result.isEmpty()) {
            resetState();
            return;
        }

        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            boolean isCatalyst = false;
            for (int id : OreDictionary.getOreIDs(stack)) {
                if (OreDictionary.getOreName(id).startsWith("rune")) {
                    isCatalyst = true;
                    break;
                }
            }
            if (!isCatalyst) continue;
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(outputView, stack.copy(), false);
            itemHandler.setStackInSlot(i, remaining);
        }

        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            itemHandler.setStackInSlot(FIRST_OUTPUT_SLOT + i, outputView.getStackInSlot(i));
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
        currentRecipe = null;
        catalysts.clear();
        manaCost = 0;
        markDirty();
    }

    // ==================== Capability ====================
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == EnumFacing.DOWN) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new OutputOnlyHandler());
            } else {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new InputOnlyHandler());
            }
        }
        return super.getCapability(capability, facing);
    }

    private class OutputOnlyHandler extends ItemStackHandler {
        OutputOnlyHandler() { super(OUTPUT_SLOTS); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= OUTPUT_SLOTS) return ItemStack.EMPTY;
            return itemHandler.extractItem(FIRST_OUTPUT_SLOT + slot, amount, simulate);
        }
        @Override public ItemStack getStackInSlot(int slot) {
            if (slot >= 0 && slot < OUTPUT_SLOTS) return itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT + slot);
            return ItemStack.EMPTY;
        }
    }

    private class InputOnlyHandler extends ItemStackHandler {
        InputOnlyHandler() { super(INPUT_SLOTS); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= INPUT_SLOTS) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack getStackInSlot(int slot) {
            if (slot >= 0 && slot < INPUT_SLOTS) return itemHandler.getStackInSlot(slot);
            return ItemStack.EMPTY;
        }
    }

    // ==================== Botania 接口 ====================
    @Override public int getCurrentMana() { return mana; }
    @Override public boolean isFull() { return mana >= MANA_CAPACITY; }
    @Override public void recieveMana(int mana) {
        this.mana = Math.min(MANA_CAPACITY, this.mana + mana);
        markDirty();
    }
    @Override public boolean canRecieveManaFromBursts() { return !isFull(); }

    // ==================== ISparkAttachable ====================
    @Override public boolean canAttachSpark(ItemStack stack) { return attachedSpark == null; }
    @Override public void attachSpark(ISparkEntity entity) { this.attachedSpark = entity; }
    @Override public ISparkEntity getAttachedSpark() { return attachedSpark; }
    @Override public boolean areIncomingTranfersDone() { return isFull(); }
    @Override public int getAvailableSpaceForMana() { return Math.max(0, MANA_CAPACITY - mana); }

    // ==================== 客户端/服务端状态 ====================
    public State getState() { return state; }
    public int getProgress() { return progress; }
    public int getManaCost() { return manaCost; }
    public int getMaxMana() { return MANA_CAPACITY; }

    // ==================== NBT ====================
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
}