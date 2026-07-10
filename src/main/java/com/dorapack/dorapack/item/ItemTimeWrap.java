package com.dorapack.dorapack.item;

import com.dorapack.dorapack.achievement.AchievementManager;
import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Time-wrapping cloth ("时光包袱皮"). Right-clicking a crop advances its growth by one stage;
 * right-clicking while holding a damaged tool in the off-hand repairs a little durability.
 * Daily-limited and durability-limited.
 */
public class ItemTimeWrap extends ItemResearchLocked {

    private static final String USAGE_KEY = "timewrap";
    private static final int TOOL_REPAIR_AMOUNT = 5;

    public ItemTimeWrap(String name) {
        super(name, 1, ResearchKeys.TIME_WRAP);
        setMaxDamage(DoraConfig.timeWrapDurability);
    }

    @Override
    @Nonnull
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (!isUnlocked(player)) {
            notifyLocked(player);
            return EnumActionResult.FAIL;
        }
        IDoraPlayerData data = DoraCapabilities.get(player);
        if (data != null) {
            data.refreshDay(DoraUtil.getWorldDay(world));
            if (data.getDailyUsage(USAGE_KEY) >= DoraConfig.timeWrapDailyLimit) {
                DoraUtil.sendActionBar(player, TextFormatting.RED + "时光包袱皮今日已用尽");
                return EnumActionResult.FAIL;
            }
        }
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof BlockCrops)) {
            return EnumActionResult.PASS;
        }
        BlockCrops crop = (BlockCrops) block;
        if (crop.isMaxAge(state)) {
            DoraUtil.sendActionBar(player, TextFormatting.YELLOW + "作物已成熟");
            return EnumActionResult.PASS;
        }
        crop.grow(world, pos, state);
        if (data != null) {
            data.incrementDailyUsage(USAGE_KEY, 1);
        }
        if (shouldConsumeDurability(player, stack)) {
            stack.damageItem(1, player);
        }
        AchievementManager.onCropGrown(player);
        return EnumActionResult.SUCCESS;
    }
}
