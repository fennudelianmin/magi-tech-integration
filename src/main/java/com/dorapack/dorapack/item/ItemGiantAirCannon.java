package com.dorapack.dorapack.item;

import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Giant air cannon ("巨大空气炮"). Hold right-click to charge: longer charge widens the blast radius
 * and increases damage, up to configured maxima. Overcharging beyond the max charge time inflicts
 * 1 heart of self-damage as a balance penalty.
 */
public class ItemGiantAirCannon extends ItemResearchLocked {

    private static final int MAX_USE_TICKS = 72000;

    public ItemGiantAirCannon(String name) {
        super(name, 1, ResearchKeys.GIANT_AIR_CANNON);
        setMaxDamage(150);
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
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @Nonnull
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return MAX_USE_TICKS;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
        if (world.isRemote || !(entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        int chargeTicks = MAX_USE_TICKS - timeLeft;
        int maxChargeTicks = DoraConfig.giantCannonMaxChargeSeconds * 20;
        double chargeRatio = Math.min(1.0D, chargeTicks / (double) maxChargeTicks);
        double range = 3.0D + (DoraConfig.giantCannonMaxRange - 3.0D) * chargeRatio;
        float damage = (float) (2.0D + (10.0D - 2.0D) * chargeRatio);

        fire(world, player, range, damage);
        if (chargeTicks > maxChargeTicks + 20) {
            player.attackEntityFrom(DamageSource.GENERIC, 2.0F);
        }
        if (shouldConsumeDurability(player, stack)) {
            stack.damageItem(1, player);
        }
    }

    private void fire(World world, EntityPlayer player, double range, float damage) {
        Vec3d look = player.getLookVec();
        AxisAlignedBB area = player.getEntityBoundingBox().grow(range);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, area,
                e -> e != null && e != player && !(e instanceof EntityPlayer) && e.isEntityAlive());
        for (EntityLivingBase target : targets) {
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
            double strength = 2.0D + range * 0.15D;
            target.addVelocity(look.x * strength, 0.45D, look.z * strength);
            target.velocityChanged = true;
        }
    }
}
