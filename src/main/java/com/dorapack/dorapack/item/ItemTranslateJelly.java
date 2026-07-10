package com.dorapack.dorapack.item;

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
 * Translation konjac ("翻译蒟蒻"). Right-click activates a 5-minute "translation" window during
 * which nearby animal sounds would be replaced by a voice pack.
 *
 * <p>Audio playback is intentionally out of scope (no audio assets), so activation only sets the
 * timed state and notifies the player; the voice-pack hook is left as a documented extension point.</p>
 */
public class ItemTranslateJelly extends ItemResearchLocked {

    private static final int DURATION_SECONDS = 300;

    public ItemTranslateJelly(String name) {
        super(name, 1, ResearchKeys.TRANSLATE_JELLY);
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
            CooldownHelper.setCooldownSeconds(stack, world, DURATION_SECONDS);
            DoraUtil.sendActionBar(player, TextFormatting.GREEN + "翻译蒟蒻生效，持续 5 分钟");
            // Extension point: begin client-side animal-sound substitution here when audio is added.
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
