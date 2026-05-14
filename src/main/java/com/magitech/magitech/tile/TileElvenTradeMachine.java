package com.magitech.magitech.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemHandlerHelper;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.mana.spark.ISparkEntity;
import vazkii.botania.api.recipe.RecipeElvenTrade;

import java.util.ArrayList;
import java.util.List;

public class TileElvenTradeMachine extends TileBase implements ISparkAttachable {

    public static final int ENERGY_CAPACITY = 20000;
    public static final int ENERGY_PER_TICK = 8;
    public static final int MANA_CAPACITY = 50000;
    public static final int DEFAULT_MANA_COST = 2000;

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    public enum State {
        IDLE,          // 等待原料
        TRADING,       // 交易中
        OUTPUT         // 产物输出
    }

    protected int mana;
    protected ISparkEntity attachedSpark;
    private State state = State.IDLE;
    private int progress;
    private int manaCost;
    private RecipeElvenTrade currentRecipe;
    private ItemStack pendingOutput;

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

        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return;

        // Check mana
        if (mana < DEFAULT_MANA_COST) return;

        // Find matching elven trade recipe
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(input.copy());

        for (RecipeElvenTrade recipe : BotaniaAPI.elvenTradeRecipes) {
            if (recipe.matches(inputs, false)) {
                currentRecipe = recipe;
                manaCost = DEFAULT_MANA_COST; // Default, can be overridden by config

                // Get output (first output)
                List<ItemStack> outputs = recipe.getOutputs();
                if (outputs.isEmpty()) continue;
                pendingOutput = outputs.get(0).copy();
                break;
            }
        }

        if (currentRecipe == null || pendingOutput == null) {
            resetState();
            return;
        }

        // Check output slot
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (!outputSlot.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(outputSlot, pendingOutput)
                || outputSlot.getCount() + pendingOutput.getCount() > outputSlot.getMaxStackSize()) {
                resetState();
                return;
            }
        }

        state = State.TRADING;
        progress = 0;
    }

    private void doTrading() {
        if (!consumeEnergy()) return;

        int manaPerTick = Math.max(1, manaCost / 20); // ~1 second
        if (mana < manaPerTick) return;

        mana -= manaPerTick;
        progress += manaPerTick;

        if (progress >= manaCost) {
            // Consume input
            itemHandler.getStackInSlot(INPUT_SLOT).shrink(1);
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
        currentRecipe = null;
        pendingOutput = null;
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
