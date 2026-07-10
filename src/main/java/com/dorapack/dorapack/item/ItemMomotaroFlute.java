package com.dorapack.dorapack.item;

import com.dorapack.dorapack.entity.EntityFairy;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.CooldownHelper;
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
 * Momotaro flute ("桃太郎笛"). Right-click summons a small fairy that attacks nearby hostile mobs
 * for 1 damage per second. The fairy's lifetime can be extended by feeding rice balls (handled by
 * the fairy entity). A short cooldown prevents summon spam.
 */
public class ItemMomotaroFlute extends ItemResearchLocked {

    private static final int SUMMON_COOLDOWN_SECONDS = 10;

    public ItemMomotaroFlute(String name) {
        super(name, 1, ResearchKeys.MOMOTARO_FLUTE);
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
        if (!CooldownHelper.isReady(stack, world)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (!world.isRemote) {
            EntityFairy fairy = new EntityFairy(world);
            fairy.setPosition(player.posX, player.posY + 1.0D, player.posZ);
            fairy.setOwner(player);
            world.spawnEntity(fairy);
            CooldownHelper.setCooldownSeconds(stack, world, SUMMON_COOLDOWN_SECONDS);
            DoraUtil.sendActionBar(player, TextFormatting.GREEN + "小精灵已召唤！");
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
