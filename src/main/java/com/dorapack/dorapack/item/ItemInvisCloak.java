package com.dorapack.dorapack.item;

import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.CooldownHelper;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Invisibility cloak ("隐身斗篷"). Right-click grants invisibility + a speed boost for a short time.
 * Attacking reveals the player (handled by {@code CombatEventHandler}). Durability-limited.
 */
public class ItemInvisCloak extends ItemResearchLocked {

    public ItemInvisCloak(String name) {
        super(name, 1, ResearchKeys.INVIS_CLOAK);
        setMaxDamage(DoraConfig.invisCloakDurability);
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
            int ticks = DoraConfig.invisCloakDurationSeconds * 20;
            player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, ticks, 0, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, ticks, 1, false, false));
            CooldownHelper.setCooldownSeconds(stack, world, DoraConfig.invisCloakDurationSeconds);
            if (shouldConsumeDurability(player, stack)) {
                stack.damageItem(1, player);
            }
            DoraUtil.sendActionBar(player, TextFormatting.AQUA + "隐身斗篷已激活");
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
