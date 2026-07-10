package com.dorapack.dorapack.item;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Memory bread ("记忆面包"). A player writes text onto the bread (right-click to store the current
 * "note" — here seeded from the item's display name); another player who eats it can read the note.
 * Daily-limited writes prevent spam. The stored note is shown in the tooltip.
 */
public class ItemMemoryBread extends ItemResearchLocked {

    private static final String KEY_NOTE = "MemoryNote";
    private static final String USAGE_KEY = "memorybread";

    public ItemMemoryBread(String name) {
        super(name, 16, ResearchKeys.MEMORY_BREAD);
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
            IDoraPlayerData data = DoraCapabilities.get(player);
            if (data != null) {
                data.refreshDay(DoraUtil.getWorldDay(world));
                if (data.getDailyUsage(USAGE_KEY) >= DoraConfig.memoryBreadDailyLimit) {
                    DoraUtil.sendActionBar(player, TextFormatting.RED + "今日记忆面包已用尽");
                    return new ActionResult<>(EnumActionResult.FAIL, stack);
                }
                data.incrementDailyUsage(USAGE_KEY, 1);
            }
            writeNote(stack, "来自 " + player.getName() + " 的记忆");
            DoraUtil.sendActionBar(player, TextFormatting.GREEN + "已写入记忆");
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    public static void writeNote(ItemStack stack, String note) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setString(KEY_NOTE, note);
        stack.setTagCompound(tag);
    }

    @Nullable
    public static String readNote(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(KEY_NOTE) ? tag.getString(KEY_NOTE) : null;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        String note = readNote(stack);
        if (note != null) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + note);
        }
    }
}
