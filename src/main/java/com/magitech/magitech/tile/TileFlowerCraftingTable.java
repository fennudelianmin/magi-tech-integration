package com.magitech.magitech.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.recipe.RecipePetals;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileFlowerCraftingTable extends TileBase {

    public static final int ENERGY_CAPACITY = 20000;
    public static final int ENERGY_PER_TICK = 8;
    public static final int WATER_PER_CRAFT = 1000;
    public static final int TANK_CAPACITY = 4000;

    private static final int SLOT_COUNT = 10;
    private static final int OUTPUT_SLOT = 10;

    public enum State {
        IDLE,          // 等待原料
        CHECKING,      // 验证配方
        WATERING,      // 注水中 (100 ticks)
        CONSUMING,     // 消耗非种子原料 (每tick一个)
        SEED,          // 消耗种子
        OUTPUT         // 产出
    }

    protected final FluidTank waterTank;
    private State state = State.IDLE;
    private int stateTimer;
    private RecipePetals currentRecipe;
    private List<ItemStack> matchedInputs;
    private ItemStack seedStack;
    private int consumeIndex;

    public TileFlowerCraftingTable() {
        super(10, 1, ENERGY_CAPACITY);
        this.waterTank = new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onContentsChanged() {
                TileFlowerCraftingTable.this.markDirty();
            }
        };
        this.stateTimer = 0;
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        switch (state) {
            case IDLE:
                tryStartCrafting();
                break;
            case CHECKING:
                validateRecipe();
                break;
            case WATERING:
                doWatering();
                break;
            case CONSUMING:
                doConsuming();
                break;
            case SEED:
                doSeed();
                break;
            case OUTPUT:
                doOutput();
                break;
        }
    }

    private void tryStartCrafting() {
        // Check energy
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) return;

        // Check water
        if (waterTank.getFluidAmount() < WATER_PER_CRAFT) return;

        // Gather all input items
        List<ItemStack> inputs = new ArrayList<>();
        seedStack = null;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputs.add(stack.copy());
            }
        }
        if (inputs.isEmpty()) return;

        // Find seed
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack stack = inputs.get(i);
            for (int id : OreDictionary.getOreIDs(stack)) {
                if ("seed".equals(OreDictionary.getOreName(id))) {
                    seedStack = stack;
                    inputs.remove(i);
                    break;
                }
            }
            if (seedStack != null) break;
        }
        if (seedStack == null) return; // No seed found

        if (inputs.isEmpty()) return; // Only seed, no recipe

        state = State.CHECKING;
        matchedInputs = inputs;
        stateTimer = 0;
    }

    private void validateRecipe() {
        // Create a temporary IItemHandler for recipe matching
        ItemStackHandler tempHandler = new ItemStackHandler(matchedInputs.size());
        for (int i = 0; i < matchedInputs.size(); i++) {
            tempHandler.setStackInSlot(i, matchedInputs.get(i));
        }

        // Match against Botania petal recipes
        for (RecipePetals recipe : BotaniaAPI.petalRecipes) {
            if (recipe.matches(tempHandler)) {
                currentRecipe = recipe;
                break;
            }
        }

        if (currentRecipe == null) {
            // No matching recipe
            resetState();
            return;
        }

        // Check output slot
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        ItemStack recipeOutput = currentRecipe.getOutput().copy();
        if (!outputSlot.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(outputSlot, recipeOutput)
                || outputSlot.getCount() + recipeOutput.getCount() > outputSlot.getMaxStackSize()) {
                resetState();
                return;
            }
        }

        state = State.WATERING;
        stateTimer = 100; // 5 seconds watering animation
    }

    private void doWatering() {
        if (!consumeEnergy()) {
            return;
        }
        stateTimer--;
        if (stateTimer <= 0) {
            // Consume water
            waterTank.drain(WATER_PER_CRAFT, true);
            state = State.CONSUMING;
            consumeIndex = 0;
        }
    }

    private void doConsuming() {
        if (!consumeEnergy()) return;
        if (consumeIndex >= matchedInputs.size()) {
            state = State.SEED;
            return;
        }
        // Consume one ingredient per tick from input slots
        ItemStack toConsume = matchedInputs.get(consumeIndex);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (ItemStack.areItemsEqual(slotStack, toConsume) && slotStack.getCount() >= toConsume.getCount()) {
                slotStack.shrink(toConsume.getCount());
                consumeIndex++;
                break;
            }
        }
        markDirty();
    }

    private void doSeed() {
        if (!consumeEnergy()) return;
        // Consume seed
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.areItemsEqual(slotStack, seedStack)) {
                slotStack.shrink(1);
                break;
            }
        }
        state = State.OUTPUT;
    }

    private void doOutput() {
        ItemStack output = currentRecipe.getOutput().copy();
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output);
        } else {
            outputSlot.grow(output.getCount());
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
        stateTimer = 0;
        currentRecipe = null;
        matchedInputs = null;
        seedStack = null;
        consumeIndex = 0;
        markDirty();
    }

    public State getState() { return state; }
    public int getStateTimer() { return stateTimer; }
    public FluidTank getWaterTank() { return waterTank; }

    @Override
    protected boolean hasCustomCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
    }

    @Nullable
    @Override
    protected <T> T getCustomCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(waterTank);
        }
        return null;
    }

    @Override
    protected void readCustomNBT(NBTTagCompound compound) {
        state = State.values()[compound.getInteger("state")];
        stateTimer = compound.getInteger("stateTimer");
        consumeIndex = compound.getInteger("consumeIndex");
        if (compound.hasKey("water")) {
            waterTank.readFromNBT(compound.getCompoundTag("water"));
        }
    }

    @Override
    protected void writeCustomNBT(NBTTagCompound compound) {
        compound.setInteger("state", state.ordinal());
        compound.setInteger("stateTimer", stateTimer);
        compound.setInteger("consumeIndex", consumeIndex);
        NBTTagCompound waterTag = new NBTTagCompound();
        waterTank.writeToNBT(waterTag);
        compound.setTag("water", waterTag);
    }
}
