package com.dorapack.dorapack.item;

import com.dorapack.dorapack.achievement.AchievementManager;
import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.item.base.ItemBase;
import com.dorapack.dorapack.research.ResearchTier;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Creative-only easter egg "智慧之王". Right-click unlocks all research (theories + every item) for
 * the user only, and triggers the "全知全能" achievement.
 */
public class ItemWisdomKing extends ItemBase {

    public ItemWisdomKing(String name) {
        super(name, 1);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        IDoraPlayerData data = DoraCapabilities.get(player);
        if (data == null) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        data.setBasicTheory(true);
        data.setAdvancedTheory(true);
        data.setSuperTheory(true);
        unlockAll(data);
        AchievementManager.onAllResearchUnlocked(player);
        DoraUtil.sendMessage(player, TextFormatting.GOLD, "你已继承哆啦A梦的智慧，全部研究已解锁！");
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private void unlockAll(IDoraPlayerData data) {
        for (ResearchTier tier : ResearchTier.values()) {
            for (String key : tier.getItemKeys()) {
                data.unlockItem(key);
            }
        }
    }
}
