package com.dorapack.dorapack.item.base;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;

/**
 * Base for research-locked items. Provides a shared guard that gameplay logic calls before applying
 * an effect: if the player has not unlocked the item's research key, the action is refused with a
 * hint. The item can still be held/crafted; only its active effect is gated.
 */
public class ItemResearchLocked extends ItemBase {

    private final String researchKey;

    public ItemResearchLocked(String name, String researchKey) {
        super(name);
        this.researchKey = researchKey;
    }

    public ItemResearchLocked(String name, int maxStackSize, String researchKey) {
        super(name, maxStackSize);
        this.researchKey = researchKey;
    }

    public String getResearchKey() {
        return researchKey;
    }

    /**
     * @return {@code true} if the player has unlocked this item's research and may use its effect
     */
    public boolean isUnlocked(@Nullable EntityPlayer player) {
        if (player == null) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        IDoraPlayerData data = DoraCapabilities.get(player);
        return data != null && data.isItemUnlocked(researchKey);
    }

    protected void notifyLocked(EntityPlayer player) {
        player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.RED + "尚未在研究台解锁该道具"), true);
    }

    protected boolean shouldConsumeDurability(EntityPlayer player, ItemStack stack) {
        return !player.isCreative();
    }
}
