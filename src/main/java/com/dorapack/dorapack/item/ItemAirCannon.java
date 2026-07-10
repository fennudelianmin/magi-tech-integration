package com.dorapack.dorapack.item;

import com.dorapack.dorapack.init.DoraCreativeTab;
import com.dorapack.dorapack.util.CooldownHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
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
 * Air cannon family ("空气炮"). Right-click fires a cone in front of the player, knocking back and
 * lightly damaging entities within {@code range} blocks. Players are never affected (PvE-safe).
 *
 * <p>The mini and normal variants only differ in range, damage, durability and cooldown, so they
 * share this implementation with different constructor parameters.</p>
 */
public class ItemAirCannon extends Item {

    private final double range;
    private final float damage;
    private final int cooldownSeconds;

    public ItemAirCannon(String name, int durability, double range, float damage, int cooldownSeconds) {
        this.range = range;
        this.damage = damage;
        this.cooldownSeconds = cooldownSeconds;
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setMaxStackSize(1);
        setMaxDamage(durability);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!CooldownHelper.isReady(stack, world)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (!world.isRemote) {
            fire(world, player, stack);
            CooldownHelper.setCooldownSeconds(stack, world, cooldownSeconds);
            if (player.getEntityWorld().getGameRules().getBoolean("keepInventory") || !player.isCreative()) {
                stack.damageItem(1, player);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private void fire(World world, EntityPlayer player, ItemStack stack) {
        Vec3d look = player.getLookVec();
        AxisAlignedBB area = player.getEntityBoundingBox().grow(range);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, area,
                e -> e != null && e != player && !(e instanceof EntityPlayer) && e.isEntityAlive());
        for (EntityLivingBase target : targets) {
            Vec3d toTarget = new Vec3d(target.posX - player.posX, 0, target.posZ - player.posZ).normalize();
            double dot = toTarget.dotProduct(new Vec3d(look.x, 0, look.z).normalize());
            if (dot < 0.3D) {
                // Only affect entities roughly in front of the player.
                continue;
            }
            if (damage > 0.0F) {
                target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
            }
            applyKnockback(player, target, look);
        }
    }

    private void applyKnockback(EntityPlayer player, Entity target, Vec3d look) {
        double strength = 1.2D + range * 0.1D;
        target.addVelocity(look.x * strength, 0.35D, look.z * strength);
        target.velocityChanged = true;
    }
}
