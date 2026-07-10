package com.dorapack.dorapack.item;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.DoraUtil;
import com.dorapack.dorapack.world.PlayerBagData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Bag expansion token ("百宝袋扩容"). Right-click spends research points to add 8 slots to the
 * player's shared bag, up to the configured maximum. The point cost scales with current size.
 */
public class ItemBagUpgrade extends ItemResearchLocked {

    private static final int SLOTS_PER_UPGRADE = 8;

    public ItemBagUpgrade(String name) {
        super(name, ResearchKeys.BAG_EXPAND);
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
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        PlayerBagData bagData = PlayerBagData.get(world);
        int currentSlots = bagData.getBag(player.getUniqueID()).getSlots();
        if (currentSlots >= DoraConfig.bagMaxSlots) {
            DoraUtil.sendActionBar(player, TextFormatting.YELLOW + "百宝袋已达到最大容量");
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        int cost = computeCost(currentSlots);
        IDoraPlayerData data = DoraCapabilities.get(player);
        if (data == null || data.getTotalPoints() < cost) {
            DoraUtil.sendActionBar(player, TextFormatting.RED + "研究点不足（需要 " + cost + "）");
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        data.setTotalPoints(data.getTotalPoints() - cost);
        int newSize = bagData.expandBag(player.getUniqueID(), SLOTS_PER_UPGRADE);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        DoraUtil.sendActionBar(player, TextFormatting.GREEN + "百宝袋扩容至 " + newSize + " 格");
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** Cost rises as the bag grows: base cost times the number of upgrades already applied (min 1). */
    public static int computeCost(int currentSlots) {
        int upgradesDone = Math.max(0, (currentSlots - DoraConfig.bagInitialSlots) / SLOTS_PER_UPGRADE);
        return DoraConfig.bagExpandCostBase * (upgradesDone + 1);
    }
}
