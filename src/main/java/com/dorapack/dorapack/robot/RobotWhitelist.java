package com.dorapack.dorapack.robot;

import com.dorapack.dorapack.entity.EntityMiningRobot;
import net.minecraft.item.ItemStack;

/**
 * Whitelist filter for the mining robot's storage. When the whitelist upgrade is installed the robot
 * only keeps items whose type already occupies one of its storage slots (the owner "seeds" the
 * whitelist by placing sample items). An empty inventory accepts everything, so a freshly upgraded
 * robot is not rendered useless until configured.
 */
public final class RobotWhitelist {

    private RobotWhitelist() {
    }

    public static boolean isAllowed(EntityMiningRobot robot, ItemStack candidate) {
        if (candidate.isEmpty()) {
            return false;
        }
        boolean anySeed = false;
        for (ItemStack slot : robot.getInventory()) {
            if (slot.isEmpty()) {
                continue;
            }
            anySeed = true;
            if (ItemStack.areItemsEqual(slot, candidate)) {
                return true;
            }
        }
        // No seed items yet: accept everything so the whitelist can be populated.
        return !anySeed;
    }
}
