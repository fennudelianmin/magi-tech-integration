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

/**
 * 符文祭坛自动化机器。
 * <p>
 * 自动执行 Botania 符文祭坛的合成。将 Botania 原版符文祭坛配方缓存到内存中，
 * 通过物品栏输入原料和魔力火花供给能量，自动匹配配方并批量合成。
 * <p>
 * 工作流程：
 *   IDLE → 检查原料和魔力 → 匹配配方 → CRAFTING
 *   CRAFTING → 每 tick 消耗魔力和 FE → 进度满后消耗原料 → OUTPUT
 *   OUTPUT → 将产物和催化剂余料写入输出槽 → IDLE
 * <p>
 * 侧面控制：
 *   上方/侧面 → 只可插入（InputOnly）
 *   下方 → 只可抽取（OutputOnly）
 */
public class TileRuneAltarMachine extends TileBase implements ISparkAttachable {

    /** 最大能量容量（FE） */
    public static final int ENERGY_CAPACITY = 40000;
    /** 每 tick 消耗的能量（FE） */
    public static final int ENERGY_PER_TICK = 16;
    /** 最大魔力容量 */
    public static final int MANA_CAPACITY = 200000;

    /** 输入槽数量（32格，存放原料和催化剂） */
    private static final int INPUT_SLOTS = 32;
    /** 输出槽数量（32格，存放产物和催化剂的余料） */
    private static final int OUTPUT_SLOTS = 32;
    /** 总槽位数 */
    private static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    /** 输出槽的起始索引（物品栏中前32个是输入，后32个是输出） */
    private static final int FIRST_OUTPUT_SLOT = INPUT_SLOTS;

    /**
     * 机器状态枚举。
     * IDLE    — 空闲，等待原料并尝试匹配配方
     * CRAFTING — 合成中，逐步消耗魔力推进进度
     * OUTPUT   — 合成完成，将产物写入输出槽
     */
    public enum State {
        IDLE,
        CRAFTING,
        OUTPUT
    }

    // ==================== 配方缓存 ====================

    /** 静态配方案缓存，启动时从 BotaniaAPI 加载所有符文祭坛配方，合并处理后放入列表 */
    private static List<CachedRecipe> recipeCache = null;

    static {
        initRecipeCache();
    }

    /**
     * 初始化配方缓存。
     * 遍历 Botania 注册的所有符文祭坛配方，将每个配方的输入按物品定义去重合并，
     * 并区分为"催化剂"（矿物词典名称以 rune 开头）和"非催化剂"两类。
     */
    private static void initRecipeCache() {
        recipeCache = new ArrayList<>();
        for (RecipeRuneAltar recipe : BotaniaAPI.runeAltarRecipes) {
            CachedRecipe cr = new CachedRecipe();
            cr.manaUsage = recipe.getManaUsage();
            cr.output = recipe.getOutput().copy();
            cr.originalRecipe = recipe;

            // 解析并合并配方输入：将同种物品合并为一个条目并统计总数量
            Map<ItemDefinition, Integer> merged = new HashMap<>();
            for (Object input : recipe.getInputs()) {
                ItemStack stack = parseInputObject(input);
                if (stack.isEmpty()) continue;
                ItemDefinition def = new ItemDefinition(stack);
                merged.put(def, merged.getOrDefault(def, 0) + stack.getCount());
            }

            // 区分配方中的催化剂和非催化剂
            for (Map.Entry<ItemDefinition, Integer> entry : merged.entrySet()) {
                ItemStack representative = entry.getKey().exampleStack;
                int count = entry.getValue();

                // 判断是否为催化剂：矿物词典名称以 "rune" 开头的视为符文催化剂
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

    /**
     * 将配方输入对象解析为 ItemStack。
     * Botania 配方输入可以是 ItemStack（具体物品）或 String（矿物词典名称）。
     */
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

    /** 缓存配方结构体：记录原料需求（分催化剂/非催化剂）、产物、魔力消耗和原始配方引用 */
    static class CachedRecipe {
        /** 配方所需的普通原料列表（已按物品类型合并数量） */
        List<ItemStack> nonCatalysts = new ArrayList<>();
        /** 配方所需的催化剂列表（符文，已合并数量） */
        List<ItemStack> catalysts = new ArrayList<>();
        /** 合成产物 */
        ItemStack output;
        /** 合成所需魔力 */
        int manaUsage;
        /** 原始 Botania 配方引用 */
        RecipeRuneAltar originalRecipe;
    }

    /**
     * 物品定义，用于配方匹配中的物品比较。
     * 先按 ItemStack 完全匹配，若失败且双方都有矿物词典 ID，
     * 则通过共享矿物词典 ID 进行兼容匹配（例如不同颜色的符文视为同类）。
     */
    static class ItemDefinition {
        /** 代表物品栈（数量固定为1，仅用于比较） */
        ItemStack exampleStack;
        /** 该物品的矿物词典 ID 列表 */
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
            // 先尝试精确匹配
            if (ItemStack.areItemsEqual(this.exampleStack, that.exampleStack)) return true;
            // 若双方都有矿物词典 ID，尝试通过共有的 ID 匹配
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
    /** 连接的火花实体，用于从火花网络接收魔力 */
    protected ISparkEntity attachedSpark;
    /** 机器当前状态 */
    private State state = State.IDLE;
    /** 当前合成进度累计消耗的魔力值 */
    private int progress;
    /** 当前正在合成的 Botania 配方 */
    private RecipeRuneAltar currentRecipe;
    /** 当前匹配到的缓存配方 */
    private CachedRecipe matchedCachedRecipe;
    /** 当前配方所需的总魔力值 */
    private int manaCost;

    public TileRuneAltarMachine() {
        super(INPUT_SLOTS, OUTPUT_SLOTS, ENERGY_CAPACITY);
        this.mana = 0;
        this.progress = 0;
    }

    /**
     * 每 tick 更新。仅在服务端执行，根据当前状态执行对应逻辑：
     * IDLE → 尝试匹配并开始合成；
     * CRAFTING → 推进合成进度；
     * OUTPUT → 输出产物。
     */
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

    /**
     * 尝试启动一次合成。
     * 步骤：收集输入槽物品 → 合并 → 匹配配方缓存 → 检查魔力 → 检查输出空间。
     * 匹配成功后进入 CRAFTING 状态。
     */
    private void tryStartCrafting() {
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 收集并合并输入槽中的物品，同种物品合并后计数
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

        // 构建输入物品的 Map（物品定义 → 数量）
        Map<ItemDefinition, Integer> inputMap = new HashMap<>();
        for (ItemStack stack : mergedInputs) {
            ItemDefinition def = new ItemDefinition(stack);
            inputMap.put(def, inputMap.getOrDefault(def, 0) + stack.getCount());
        }

        // 匹配配方缓存
        currentRecipe = null;
        matchedCachedRecipe = null;

        for (CachedRecipe cr : recipeCache) {
            // 检查魔力是否足够
            if (mana < cr.manaUsage) continue;

            Map<ItemDefinition, Integer> remaining = new HashMap<>(inputMap);

            // 检查非催化剂是否足够
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

            // 检查催化剂是否足够
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

            currentRecipe = cr.originalRecipe;
            manaCost = cr.manaUsage;
            matchedCachedRecipe = cr;
            break;
        }

        if (currentRecipe == null) return;
        System.out.println("匹配到的配方：" + currentRecipe);

        // 检查输出槽是否有足够空间容纳产物和催化剂余料
        if (!canFitOutputs(currentRecipe.getOutput(), matchedCachedRecipe.catalysts)) {
            currentRecipe = null;
            matchedCachedRecipe = null;
            return;
        }

        state = State.CRAFTING;
        progress = 0;
    }

    /**
     * 模拟检查输出槽能否容纳产物和催化剂。
     * 使用一个临时物品栏副本进行模拟插入以避免真正改变数据。
     */
    private boolean canFitOutputs(ItemStack output, List<ItemStack> recipeCatalysts) {
        ItemStackHandler tempOutput = new ItemStackHandler(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            tempOutput.setStackInSlot(i, itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT + i).copy());
        }

        ItemStack remaining = ItemHandlerHelper.insertItemStacked(tempOutput, output.copy(), true);
        if (!remaining.isEmpty()) return false;

        for (ItemStack cat : recipeCatalysts) {
            remaining = ItemHandlerHelper.insertItemStacked(tempOutput, cat.copy(), true);
            if (!remaining.isEmpty()) return false;
        }
        return true;
    }

    /**
     * 推进合成进度。
     * 每次消耗 ENERGY_PER_TICK 的 FE 和一定量的魔力，累计到进度中。
     * 当进度达到所需魔力总量时，从输入槽扣除非催化剂原料，进入 OUTPUT 状态。
     */
    private void doCrafting() {
        if (!consumeEnergy()) return;
        int manaPerTick = Math.max(1, manaCost / 60);
        if (mana < manaPerTick) return;

        mana -= manaPerTick;
        progress += manaPerTick;

        if (progress >= manaCost) {
            // 精确消耗非催化剂：只扣除配方所需的一份（数量配方的需求量）
            List<ItemStack> toConsume = new ArrayList<>();
            for (ItemStack need : matchedCachedRecipe.nonCatalysts) {
                toConsume.add(need.copy());
            }

            // 遍历输入槽，逐一扣除对应原料
            for (int i = 0; i < INPUT_SLOTS && !toConsume.isEmpty(); i++) {
                ItemStack slotStack = itemHandler.getStackInSlot(i);
                if (slotStack.isEmpty()) continue;

                // 跳过催化剂（符文不应被消耗）
                boolean isCatalyst = false;
                for (int id : OreDictionary.getOreIDs(slotStack)) {
                    if (OreDictionary.getOreName(id).startsWith("rune")) {
                        isCatalyst = true;
                        break;
                    }
                }
                if (isCatalyst) continue;

                // 尝试从这个槽扣除所需的原料
                for (Iterator<ItemStack> it = toConsume.iterator(); it.hasNext(); ) {
                    ItemStack need = it.next();
                    if (ItemHandlerHelper.canItemStacksStack(slotStack, need)) {
                        int toRemove = Math.min(slotStack.getCount(), need.getCount());
                        slotStack.shrink(toRemove);
                        need.shrink(toRemove);
                        if (slotStack.getCount() <= 0) {
                            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                        }
                        if (need.getCount() <= 0) {
                            it.remove();
                        }
                        break;
                    }
                }
            }

            state = State.OUTPUT;
        }
        markDirty();
    }

    /**
     * 输出产物并处理催化剂余料。
     * 将产物插入输出槽，然后从输入槽中取走一份配方的催化剂，
     * 插入到输出槽以供玩家/管道取回。
     * 完成后重置状态回到 IDLE。
     */
    private void doOutput() {
        // 构建输出区视图
        ItemStackHandler outputView = new ItemStackHandler(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            outputView.setStackInSlot(i, itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT + i));
        }

        // 插入产物
        ItemStack result = ItemHandlerHelper.insertItemStacked(outputView, currentRecipe.getOutput().copy(), false);
        if (!result.isEmpty()) {
            resetState();
            return;
        }

        // 从输入槽抽取配方所需的催化剂，移动到输出区（只移一份）
        List<ItemStack> recipeCatalysts = matchedCachedRecipe.catalysts;
        for (ItemStack needed : recipeCatalysts) {
            int stillNeed = needed.getCount();
            for (int i = 0; i < INPUT_SLOTS && stillNeed > 0; i++) {
                ItemStack inSlot = itemHandler.getStackInSlot(i);
                if (inSlot.isEmpty()) continue;
                if (!ItemHandlerHelper.canItemStacksStack(inSlot, needed)) continue;

                int toTake = Math.min(stillNeed, inSlot.getCount());
                ItemStack taken = inSlot.splitStack(toTake);
                if (inSlot.getCount() <= 0) {
                    itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }

                ItemStack leftover = ItemHandlerHelper.insertItemStacked(outputView, taken, false);
                if (!leftover.isEmpty()) {
                    // 极罕见情况：输出区空间不足，将未放下的催化剂放回原槽
                    itemHandler.insertItem(i, leftover, false);
                }
                stillNeed -= (toTake - leftover.getCount());
            }
        }

        // 写回输出槽
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            itemHandler.setStackInSlot(FIRST_OUTPUT_SLOT + i, outputView.getStackInSlot(i));
        }

        resetState();
    }

    /** 每 tick 消耗 FE 能量，若能量不足则返回 false */
    private boolean consumeEnergy() {
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return false;
        energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        return true;
    }

    /** 重置机器状态为 IDLE，清空合成相关的临时字段 */
    private void resetState() {
        state = State.IDLE;
        progress = 0;
        currentRecipe = null;
        matchedCachedRecipe = null;
        manaCost = 0;
        markDirty();
    }

    // ==================== Capability 侧面限制 ====================

    /**
     * 覆写基类的 getCapability，根据方向返回不同的物品栏处理器：
     * 下方 → 只可抽取（OutputOnly）
     * 其他面 → 只可插入（InputOnly）
     */
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

    /** 只输出处理器：暴露输出槽，禁止插入，只允许抽取 */
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

    /** 只输入处理器：暴露输入槽，禁止抽取，只允许插入 */
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

    // ==================== Botania 魔力接口 ====================

    @Override public int getCurrentMana() { return mana; }
    @Override public boolean isFull() { return mana >= MANA_CAPACITY; }
    @Override public void recieveMana(int mana) {
        this.mana = Math.min(MANA_CAPACITY, this.mana + mana);
        markDirty();
    }
    @Override public boolean canRecieveManaFromBursts() { return !isFull(); }

    // ==================== ISparkAttachable（火花接口） ====================

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
}
