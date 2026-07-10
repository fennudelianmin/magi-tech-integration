package com.dorapack.dorapack.item;

import com.dorapack.dorapack.config.DoraConfig;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import com.dorapack.dorapack.research.ResearchKeys;
import com.dorapack.dorapack.util.CooldownHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Rocket booster ("火箭推进器"). Right-click dashes the player a fixed distance in their look
 * direction (horizontal or vertical). Has a cooldown; wall impacts cause fall/impact damage handled
 * by vanilla physics.
 */
public class ItemRocketBooster extends ItemResearchLocked {

    public ItemRocketBooster(String name) {
        super(name, 1, ResearchKeys.ROCKET_BOOSTER);
        setMaxDamage(200);
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
        Vec3d look = player.getLookVec();
        double power = DoraConfig.rocketBoosterDistance / 10.0D;
        player.motionX = look.x * power;
        player.motionY = look.y * power + 0.2D;
        player.motionZ = look.z * power;
        player.velocityChanged = true;
        player.fallDistance = 0.0F;
        if (!world.isRemote) {
            CooldownHelper.setCooldownSeconds(stack, world, DoraConfig.rocketBoosterCooldownSeconds);
            if (shouldConsumeDurability(player, stack)) {
                stack.damageItem(1, player);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
