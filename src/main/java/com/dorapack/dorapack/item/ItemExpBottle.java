package com.dorapack.dorapack.item;

import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Experience storage bottle ("经验存储瓶"). Right-click deposits the player's XP (up to capacity);
 * sneak-right-click withdraws stored XP back to the player. Stored amount is kept in stack NBT.
 */
public class ItemExpBottle extends ItemResearchLocked {

    private static final String KEY_STORED = "StoredExp";

    public ItemExpBottle(String name) {
        super(name, 1, ResearchKeys.EXP_BOTTLE);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!isUnlocked(player)) {
            if (!world.isRemote) {
                notifyLocked(player);
            }
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (!world.isRemote) {
            if (player.isSneaking()) {
                withdraw(player, stack);
            } else {
                deposit(player, stack);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private void deposit(EntityPlayer player, ItemStack stack) {
        int stored = getStored(stack);
        int room = DoraConfig.expBottleCapacity - stored;
        if (room <= 0) {
            DoraUtil.sendActionBar(player, TextFormatting.YELLOW + "瓶子已满（" + stored + "）");
            return;
        }
        int playerXp = getPlayerTotalXp(player);
        int move = Math.min(room, playerXp);
        if (move <= 0) {
            DoraUtil.sendActionBar(player, TextFormatting.YELLOW + "没有可存储的经验");
            return;
        }
        addPlayerXp(player, -move);
        setStored(stack, stored + move);
        DoraUtil.sendActionBar(player, TextFormatting.GREEN + "已存入 " + move + " 点经验（共 " + (stored + move) + "）");
    }

    private void withdraw(EntityPlayer player, ItemStack stack) {
        int stored = getStored(stack);
        if (stored <= 0) {
            DoraUtil.sendActionBar(player, TextFormatting.YELLOW + "瓶子是空的");
            return;
        }
        addPlayerXp(player, stored);
        setStored(stack, 0);
        DoraUtil.sendActionBar(player, TextFormatting.GREEN + "已取回 " + stored + " 点经验");
    }

    private int getStored(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.getInteger(KEY_STORED);
    }

    private void setStored(ItemStack stack, int value) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setInteger(KEY_STORED, Math.max(0, value));
        stack.setTagCompound(tag);
    }

    /** Total experience points a player currently holds (levels + partial bar). */
    private int getPlayerTotalXp(EntityPlayer player) {
        return player.experienceTotal;
    }

    private void addPlayerXp(EntityPlayer player, int amount) {
        player.addExperience(amount);
    }
}
