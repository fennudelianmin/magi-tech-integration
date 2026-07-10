package com.dorapack.dorapack.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Per-ItemStack cooldown tracking stored in the stack's NBT as an absolute "ready" world time.
 *
 * <p>This avoids any global tick listener: readiness is computed on demand from the world time.</p>
 */
public final class CooldownHelper {

    private static final String KEY_READY_TIME = "DoraCooldownReady";

    private CooldownHelper() {
    }

    public static boolean isReady(ItemStack stack, World world) {
        if (!stack.hasTagCompound()) {
            return true;
        }
        long ready = stack.getTagCompound().getLong(KEY_READY_TIME);
        return world.getTotalWorldTime() >= ready;
    }

    public static long getRemainingTicks(ItemStack stack, World world) {
        if (!stack.hasTagCompound()) {
            return 0L;
        }
        long ready = stack.getTagCompound().getLong(KEY_READY_TIME);
        return Math.max(0L, ready - world.getTotalWorldTime());
    }

    public static void setCooldownSeconds(ItemStack stack, World world, int seconds) {
        setCooldownTicks(stack, world, seconds * 20L);
    }

    public static void setCooldownTicks(ItemStack stack, World world, long ticks) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setLong(KEY_READY_TIME, world.getTotalWorldTime() + Math.max(0L, ticks));
        stack.setTagCompound(tag);
    }
}
