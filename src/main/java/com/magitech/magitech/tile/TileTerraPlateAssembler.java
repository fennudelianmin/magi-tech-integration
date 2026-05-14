package com.magitech.magitech.tile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemHandlerHelper;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.mana.spark.ISparkEntity;

public class TileTerraPlateAssembler extends TileBase implements ISparkAttachable {

    public static final int ENERGY_CAPACITY = 80000;
    public static final int ENERGY_PER_TICK = 32;
    public static final int MANA_CAPACITY = 1000000;
    public static final int MANA_COST_PER_CRAFT = 500000;

    // 槽位布局: 0=center, 1=north, 2=east, 3=south, 4=west
    // 5-9=extra livingrock slots, 10=output
    private static final int SLOT_CENTER = 0;
    private static final int SLOT_NORTH = 1;
    private static final int SLOT_EAST = 2;
    private static final int SLOT_SOUTH = 3;
    private static final int SLOT_WEST = 4;
    private static final int INPUT_SLOTS = 10;
    private static final int OUTPUT_SLOT = 10;

    public enum State {
        IDLE,          // 等待原料
        CRAFTING,      // 合成中
        OUTPUT         // 产物输出
    }

    protected int mana;
    protected ISparkEntity attachedSpark;
    private State state = State.IDLE;
    private int progress;
    private int manaConsumed;

    public TileTerraPlateAssembler() {
        super(INPUT_SLOTS, 1, ENERGY_CAPACITY);
        this.mana = 0;
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

        // Check center slot has valid ingredient (magic steel/diamond/pearl)
        ItemStack center = itemHandler.getStackInSlot(SLOT_CENTER);
        if (center.isEmpty()) return;

        // Check mana
        if (mana < MANA_COST_PER_CRAFT) return;

        // Check at least one livingrock is present
        boolean hasLivingrock = false;
        for (int i = SLOT_NORTH; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && isLivingrock(stack)) {
                hasLivingrock = true;
                break;
            }
        }
        if (!hasLivingrock) return;

        // Check output slot
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        Item terrasteel = Item.getByNameOrId("botania:terrasteel");
        if (terrasteel == null) return;
        ItemStack terrasteelStack = new ItemStack(terrasteel);

        if (!outputSlot.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(outputSlot, terrasteelStack)
                || outputSlot.getCount() + 1 > outputSlot.getMaxStackSize()) {
                return;
            }
        }

        state = State.CRAFTING;
        progress = 0;
        manaConsumed = 0;
    }

    private void doCrafting() {
        if (!consumeEnergy()) return;

        int manaPerTick = Math.max(1, MANA_COST_PER_CRAFT / 120); // ~6 seconds
        if (mana < manaPerTick) return; // Wait for mana

        mana -= manaPerTick;
        manaConsumed += manaPerTick;
        progress = (manaConsumed * 100) / MANA_COST_PER_CRAFT;

        if (manaConsumed >= MANA_COST_PER_CRAFT) {
            // Consume center ingredient (magic steel/diamond/pearl → terrasteel)
            itemHandler.getStackInSlot(SLOT_CENTER).shrink(1);

            // Livingrock is NOT consumed (returned alongside output)
            state = State.OUTPUT;
        }
        markDirty();
    }

    private void doOutput() {
        Item terrasteel = Item.getByNameOrId("botania:terrasteel");
        if (terrasteel == null) {
            resetState();
            return;
        }
        ItemStack output = new ItemStack(terrasteel);
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output);
        } else {
            outputSlot.grow(1);
        }
        // 活石保留在输入槽中，可被输出总线抽走
        resetState();
    }

    private boolean consumeEnergy() {
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return false;
        energyStorage.extractEnergy(ENERGY_PER_TICK, false);
        return true;
    }

    private boolean isLivingrock(ItemStack stack) {
        Item livingrock = Item.getByNameOrId("botania:livingrock");
        return livingrock != null && stack.getItem() == livingrock;
    }

    private void resetState() {
        state = State.IDLE;
        progress = 0;
        manaConsumed = 0;
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
    public int getMaxMana() { return MANA_CAPACITY; }

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
