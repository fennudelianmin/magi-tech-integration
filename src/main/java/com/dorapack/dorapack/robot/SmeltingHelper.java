package com.dorapack.dorapack.robot;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.world.World;

/**
 * Applies the furnace recipe to a mined drop when the smelting upgrade is installed. If no smelting
 * result exists the original stack is returned unchanged.
 */
public final class SmeltingHelper {

    private SmeltingHelper() {
    }

    public static ItemStack smelt(World world, ItemStack input) {
        if (input.isEmpty()) {
            return input;
        }
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(input);
        if (result.isEmpty()) {
            return input;
        }
        ItemStack smelted = result.copy();
        smelted.setCount(result.getCount() * input.getCount());
        return smelted;
    }
}
