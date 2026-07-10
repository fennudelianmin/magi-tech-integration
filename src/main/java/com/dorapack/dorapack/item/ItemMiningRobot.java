package com.dorapack.dorapack.item;

import com.dorapack.dorapack.entity.EntityMiningRobot;
import com.dorapack.dorapack.item.base.ItemResearchLocked;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Deployable mining robot ("采矿机器人") item. Right-clicking a block face spawns an
 * {@link EntityMiningRobot} owned by the placing player and consumes the item (unless creative).
 * Gated behind the super-theory tier-3 research.
 */
public class ItemMiningRobot extends ItemResearchLocked {

    public ItemMiningRobot(String name, String researchKey) {
        super(name, 16, researchKey);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!isUnlocked(player)) {
            notifyLocked(player);
            return EnumActionResult.FAIL;
        }
        BlockPos spawnPos = pos.offset(facing);
        EntityMiningRobot robot = new EntityMiningRobot(world);
        robot.setPosition(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        robot.setOwner(player);
        world.spawnEntity(robot);
        if (shouldConsumeDurability(player, player.getHeldItem(hand))) {
            player.getHeldItem(hand).shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
