package com.magitech.magitech.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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

    private static final int INPUT_SLOTS = 8;
    private static final int OUTPUT_SLOT = 8;

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
        super(INPUT_SLOTS, 1, ENERGY_CAPACITY);
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
        // Check energy
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // Gather inputs
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }
        if (inputs.isEmpty()) return;

        // Create temp handler for recipe matching
        ItemStackHandler tempHandler = new ItemStackHandler(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            tempHandler.setStackInSlot(i, inputs.get(i));
        }

        // Find matching recipe
        for (RecipeRuneAltar recipe : BotaniaAPI.runeAltarRecipes) {
            if (recipe.matches(tempHandler)) {
                currentRecipe = recipe;
                manaCost = recipe.getManaUsage();
                break;
            }
        }
        if (currentRecipe == null) return;

        // Check mana availability
        if (mana < manaCost) return;

        // Identify catalysts (rune-named items remain; non-rune items are consumed)
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

        // Check output slot
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!outputSlot.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(outputSlot, currentRecipe.getOutput())
                || outputSlot.getCount() + currentRecipe.getOutput().getCount() > outputSlot.getMaxStackSize()) {
                resetState();
                return;
            }
        }

        state = State.CRAFTING;
        progress = 0;
    }

    private void doCrafting() {
        if (!consumeEnergy()) return;

        // 每tick消耗总魔力的一部分，速度由speed_multiplier控制
        int manaPerTick = Math.max(1, manaCost / 60); // 约3秒完成 (60 ticks)
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
                // 催化剂保留在输入槽中，稍后移动到输出
            }

            state = State.OUTPUT;
        }
        markDirty();
    }

    private void doOutput() {
        // 产出产物
        ItemStack output = currentRecipe.getOutput().copy();
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output);
        } else {
            outputSlot.grow(output.getCount());
        }

        // 催化剂也移到输出槽 (如果输出槽有空间)
        // 实际上催化剂已经在输入槽中被保留了，它们可以留在原地被取出
        // 或者移动到输出区域

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

    // --- IManaBlock ---
    @Override
    public int getCurrentMana() { return mana; }

    // --- IManaReceiver ---
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

    // --- State getters ---
    public State getState() { return state; }
    public int getProgress() { return progress; }
    public int getManaCost() { return manaCost; }
    public int getMaxMana() { return MANA_CAPACITY; }

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
