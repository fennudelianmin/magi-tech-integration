package com.dorapack.dorapack.item;

import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.CooldownHelper;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Time-stop watch ("时间暂停表"). Right-click freezes nearby entities (not the player) for a few
 * seconds by applying extreme slowness + mining fatigue. Costs 50% of the player's current health
 * and has a long cooldown.
 */
public class ItemTimeStopWatch extends ItemResearchLocked {

    public ItemTimeStopWatch(String name) {
        super(name, 1, ResearchKeys.TIME_STOP_WATCH);
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
            long remaining = CooldownHelper.getRemainingTicks(stack, world) / 20L;
            if (!world.isRemote) {
                DoraUtil.sendActionBar(player, TextFormatting.YELLOW + "时间暂停表冷却中：" + remaining + " 秒");
            }
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (!world.isRemote) {
            float cost = player.getHealth() * 0.5F;
            if (player.getHealth() - cost < 1.0F) {
                DoraUtil.sendActionBar(player, TextFormatting.RED + "生命值不足以承受时间暂停");
                return new ActionResult<>(EnumActionResult.FAIL, stack);
            }
            freezeNearby(world, player);
            player.setHealth(player.getHealth() - cost);
            CooldownHelper.setCooldownSeconds(stack, world, DoraConfig.timeStopCooldownMinutes * 60);
            DoraUtil.sendMessage(player, TextFormatting.LIGHT_PURPLE, "时间暂停！");
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private void freezeNearby(World world, EntityPlayer player) {
        int durationTicks = DoraConfig.timeStopSeconds * 20;
        AxisAlignedBB area = player.getEntityBoundingBox().grow(DoraConfig.timeStopRadius);
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, area,
                e -> e != null && e != player && e.isEntityAlive());
        for (EntityLivingBase entity : entities) {
            entity.addPotionEffect(new PotionEffect(net.minecraft.init.MobEffects.SLOWNESS, durationTicks, 250, false, false));
            entity.addPotionEffect(new PotionEffect(net.minecraft.init.MobEffects.MINING_FATIGUE, durationTicks, 250, false, false));
            entity.addPotionEffect(new PotionEffect(net.minecraft.init.MobEffects.WEAKNESS, durationTicks, 250, false, false));
        }
    }
}
