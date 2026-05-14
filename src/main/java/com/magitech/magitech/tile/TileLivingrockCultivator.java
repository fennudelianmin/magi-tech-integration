package com.magitech.magitech.tile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

public class TileLivingrockCultivator extends TileBase {

    public static final int ENERGY_CAPACITY = 10000;
    public static final int ENERGY_PER_TICK = 4;
    public static final int PROCESS_TIME = 1200; // 60 seconds

    private int progress;
    private String currentRecipeType; // "stone" or "logWood" or null

    public TileLivingrockCultivator() {
        super(1, 1, ENERGY_CAPACITY);
        this.progress = 0;
        this.currentRecipeType = null;
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        ItemStack input = itemHandler.getStackInSlot(0);
        ItemStack output = itemHandler.getStackInSlot(1);

        if (input.isEmpty()) {
            // No input, reset progress
            progress = 0;
            currentRecipeType = null;
            return;
        }

        // Determine recipe type from input
        String oreName = getValidOreName(input);
        if (oreName == null) {
            progress = 0;
            currentRecipeType = null;
            return;
        }

        // Check if we need to reset due to recipe change
        if (!oreName.equals(currentRecipeType)) {
            progress = 0;
            currentRecipeType = oreName;
        }

        ItemStack targetOutput = getOutputForOre(oreName);
        if (targetOutput == null) {
            progress = 0;
            return;
        }

        // Check output slot compatibility
        if (!output.isEmpty()) {
            if (!ItemStack.areItemsEqual(output, targetOutput)
                || output.getCount() >= output.getMaxStackSize()) {
                return; // Output slot blocked
            }
        }

        // Check and consume energy
        if (energyStorage.getEnergyStored() < ENERGY_PER_TICK) {
            return; // Not enough energy
        }
        energyStorage.extractEnergy(ENERGY_PER_TICK, false);

        // Progress the crafting
        progress++;
        if (progress >= PROCESS_TIME) {
            // Craft complete
            input.shrink(1);
            if (output.isEmpty()) {
                itemHandler.setStackInSlot(1, targetOutput.copy());
            } else {
                output.grow(1);
            }
            progress = 0;
            currentRecipeType = null;
        }
        markDirty();
    }

    private String getValidOreName(ItemStack stack) {
        for (int id : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(id);
            if ("stone".equals(name)) return "stone";
            if ("logWood".equals(name)) return "logWood";
        }
        return null;
    }

    private ItemStack getOutputForOre(String oreName) {
        if ("stone".equals(oreName)) {
            Item livingrock = Item.getByNameOrId("botania:livingrock");
            if (livingrock != null) return new ItemStack(livingrock);
        } else if ("logWood".equals(oreName)) {
            Item livingwood = Item.getByNameOrId("botania:livingwood");
            if (livingwood != null) return new ItemStack(livingwood);
        }
        return null;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return PROCESS_TIME;
    }

    @Override
    protected void readCustomNBT(NBTTagCompound compound) {
        progress = compound.getInteger("progress");
        if (compound.hasKey("recipeType")) {
            currentRecipeType = compound.getString("recipeType");
        }
    }

    @Override
    protected void writeCustomNBT(NBTTagCompound compound) {
        compound.setInteger("progress", progress);
        if (currentRecipeType != null) {
            compound.setString("recipeType", currentRecipeType);
        }
    }
}
