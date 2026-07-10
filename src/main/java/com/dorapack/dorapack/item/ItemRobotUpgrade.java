package com.dorapack.dorapack.item;

import com.dorapack.dorapack.entity.EntityMiningRobot;
import com.dorapack.dorapack.init.DoraCreativeTab;
import com.dorapack.dorapack.robot.RobotUpgrade;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

/**
 * Mining-robot upgrade module item ("升级模块"). One item with a sub-type per {@link RobotUpgrade}
 * (metadata = enum ordinal). Right-clicking a robot with a module installs one level of that upgrade,
 * consuming the item; the module is refused (with feedback) when the robot is already at the cap.
 */
public class ItemRobotUpgrade extends Item {

    public ItemRobotUpgrade(String name) {
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(DoraCreativeTab.INSTANCE);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    public static RobotUpgrade upgradeFor(ItemStack stack) {
        int meta = stack.getMetadata();
        RobotUpgrade[] values = RobotUpgrade.values();
        return meta >= 0 && meta < values.length ? values[meta] : null;
    }

    @Override
    @Nonnull
    public String getTranslationKey(ItemStack stack) {
        RobotUpgrade upgrade = upgradeFor(stack);
        return super.getTranslationKey(stack) + (upgrade == null ? "" : "." + upgrade.getId());
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player,
                                            EntityLivingBase target, EnumHand hand) {
        if (!(target instanceof EntityMiningRobot)) {
            return false;
        }
        EntityMiningRobot robot = (EntityMiningRobot) target;
        if (player.world.isRemote) {
            return true;
        }
        if (!robot.isOwner(player) && !player.isCreative()) {
            return false;
        }
        RobotUpgrade upgrade = upgradeFor(stack);
        if (upgrade == null) {
            return false;
        }
        if (!robot.getStats().addLevel(upgrade)) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "该模块已达最大等级"), true);
            return false;
        }
        robot.resizeInventoryIfNeeded();
        robot.setEnergy(robot.getEnergy());
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return true;
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) {
            return;
        }
        RobotUpgrade[] values = RobotUpgrade.values();
        for (int i = 0; i < values.length; i++) {
            items.add(new ItemStack(this, 1, i));
        }
    }
}
