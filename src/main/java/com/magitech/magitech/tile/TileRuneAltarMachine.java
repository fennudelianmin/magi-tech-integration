package com.magitech.magitech.tile;

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

import java.util.ArrayList;
import java.util.List;

public class TileRuneAltarMachine extends TileBase implements ISparkAttachable {

    public static final int ENERGY_CAPACITY = 40000;
    public static final int ENERGY_PER_TICK = 16;
    public static final int MANA_CAPACITY = 200000;

    // 槽位配置：32个输入，32个输出
    private static final int INPUT_SLOTS = 32;
    private static final int OUTPUT_SLOTS = 32;
    private static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS; // 64
    private static final int FIRST_OUTPUT_SLOT = INPUT_SLOTS; // 32

    public enum State {
        IDLE,          // 等待原料和魔力
        CRAFTING,      // 合成中 (消耗魔力)
        OUTPUT         // 产物输出
    }

    protected int mana;
    protected ISparkEntity attachedSpark;
    private State state = State.IDLE;
    private int progress;
    private RecipeRuneAltar currentRecipe;
    private List<ItemStack> catalysts; // 催化剂物品，合成后返回
    private int manaCost;

    public TileRuneAltarMachine() {
        // 假设父类构造参数为 (输入槽数, 输出槽数, 能量容量)
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
        // 检查能量
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // 收集输入槽物品
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }
        if (inputs.isEmpty()) return;

        // 创建临时Handler用于配方匹配
        ItemStackHandler tempHandler = new ItemStackHandler(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            tempHandler.setStackInSlot(i, inputs.get(i));
        }

        // 查找配方
        currentRecipe = null;
        for (RecipeRuneAltar recipe : BotaniaAPI.runeAltarRecipes) {
            if (recipe.matches(tempHandler)) {
                currentRecipe = recipe;
                manaCost = recipe.getManaUsage();
                break;
            }
        }
        if (currentRecipe == null) return;

        // 检查魔力
        if (mana < manaCost) return;

        // 识别催化剂（矿物名以"rune"开头）
        catalysts.clear();
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack stack = inputs.get(i);
            boolean isCatalyst = false;
            for (int id : OreDictionary.getOreIDs(stack)) {
                String oreName = OreDictionary.getOreName(id);
                if (oreName.startsWith("rune")) {
                    isCatalyst = true;
                    break;
                }
            }
            if (isCatalyst) {
                catalysts.add(stack.copy());
            }
        }

        // 模拟将产物和所有催化剂插入输出槽区域，检查空间是否足够
        if (!canFitOutputs(currentRecipe.getOutput(), catalysts)) {
            currentRecipe = null; // 空间不足，放弃本次匹配
            return;
        }

        state = State.CRAFTING;
        progress = 0;
    }

    /**
     * 模拟插入产物和催化剂列表，检查输出槽是否有足够空间。
     */
    private boolean canFitOutputs(ItemStack output, List<ItemStack> catalysts) {
        // 创建临时输出槽视图（仅用于模拟）
        ItemStackHandler tempOutput = new ItemStackHandler(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            tempOutput.setStackInSlot(i, itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT + i).copy());
        }

        // 模拟插入产物
        ItemStack remaining = ItemHandlerHelper.insertItemStacked(tempOutput, output.copy(), true);
        if (!remaining.isEmpty()) {
            return false; // 产物无法完全放入
        }

        // 模拟插入每个催化剂
        for (ItemStack catalyst : catalysts) {
            remaining = ItemHandlerHelper.insertItemStacked(tempOutput, catalyst.copy(), true);
            if (!remaining.isEmpty()) {
                return false; // 催化剂无法完全放入
            }
        }
        return true;
    }

    private void doCrafting() {
        if (!consumeEnergy()) return;

        // 每tick消耗的魔力，保证约3秒（60 ticks）完成
        int manaPerTick = Math.max(1, manaCost / 60);
        if (mana < manaPerTick) return;

        mana -= manaPerTick;
        progress += manaPerTick;

        if (progress >= manaCost) {
            // 消耗所有非催化剂原料
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
        // 临时输出槽视图（用于实际插入）
        ItemStackHandler outputView = new ItemStackHandler(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            outputView.setStackInSlot(i, itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT + i));
        }

        // 插入产物
        ItemStack result = ItemHandlerHelper.insertItemStacked(outputView, currentRecipe.getOutput().copy(), false);
        if (!result.isEmpty()) {
            // 理论上不应发生（开始前已模拟检查），若发生则重置，避免物品丢失
            resetState();
            return;
        }

        // 将输入槽中的催化剂全部移入输出区域
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 确认是催化剂（此处应该都是，非催化剂已被清空）
            boolean isCatalyst = false;
            for (int id : OreDictionary.getOreIDs(stack)) {
                if (OreDictionary.getOreName(id).startsWith("rune")) {
                    isCatalyst = true;
                    break;
                }
            }
            if (!isCatalyst) continue;

            // 尝试插入输出区，剩下的放回输入槽（空间不足时保护物品）
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(outputView, stack.copy(), false);
            itemHandler.setStackInSlot(i, remaining); // 空或剩余部分
        }

        // 将临时视图写回实际输出槽
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

    // ==================== 侧面能力（自动化限制） ====================
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == EnumFacing.DOWN) {
                // 底面：只能提取输出槽
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new OutputOnlyHandler());
            } else {
                // 其他面：只能插入输入槽
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new InputOnlyHandler());
            }
        }
        return super.getCapability(capability, facing);
    }

    /**
     * 仅暴露输出槽区域，且只能提取不能插入。
     */
    private class OutputOnlyHandler extends ItemStackHandler {
        OutputOnlyHandler() {
            super(OUTPUT_SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack; // 拒绝插入
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= OUTPUT_SLOTS) return ItemStack.EMPTY;
            return itemHandler.extractItem(FIRST_OUTPUT_SLOT + slot, amount, simulate);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot >= 0 && slot < OUTPUT_SLOTS) {
                return itemHandler.getStackInSlot(FIRST_OUTPUT_SLOT + slot);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlots() {
            return OUTPUT_SLOTS;
        }
    }

    /**
     * 仅暴露输入槽区域，且只能插入不能提取。
     */
    private class InputOnlyHandler extends ItemStackHandler {
        InputOnlyHandler() {
            super(INPUT_SLOTS);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // 禁止提取
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= INPUT_SLOTS) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot >= 0 && slot < INPUT_SLOTS) {
                return itemHandler.getStackInSlot(slot);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlots() {
            return INPUT_SLOTS;
        }
    }

    // ==================== Botania 魔力接口 ====================
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

    // ==================== ISparkAttachable ====================
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

    // ==================== 客户端/服务端状态获取 ====================
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