package com.dorapack.dorapack.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * Small shared helpers for player feedback and world-day math.
 */
public final class DoraUtil {

    public static final int TICKS_PER_DAY = 24000;

    private DoraUtil() {
    }

    /** Sends a status-bar (action bar) message, avoiding chat spam for frequent feedback. */
    public static void sendActionBar(EntityPlayer player, String message) {
        player.sendStatusMessage(new TextComponentString(message), true);
    }

    public static void sendMessage(EntityPlayer player, TextFormatting color, String message) {
        player.sendMessage(new TextComponentString(color + message));
    }

    /** @return the current world "day" number, used to roll over daily limits. */
    public static long getWorldDay(net.minecraft.world.World world) {
        return world.getTotalWorldTime() / TICKS_PER_DAY;
    }
}
