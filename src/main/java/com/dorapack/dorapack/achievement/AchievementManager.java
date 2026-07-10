package com.dorapack.dorapack.achievement;

import com.dorapack.dorapack.capability.DoraCapabilities;
import com.dorapack.dorapack.capability.IDoraPlayerData;
import com.dorapack.dorapack.util.DoraUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

/**
 * Central achievement progress hub. Gameplay code calls the {@code onXxx} hooks; this class updates
 * the player's counter, checks the threshold, and — on completion — awards research points and
 * notifies the player. Achievements are one-shot (guarded by the completed set).
 */
public final class AchievementManager {

    private AchievementManager() {
    }

    public static void onDoorTeleport(EntityPlayer player) {
        progress(player, AchievementKeys.DOOR_GOD, 1);
    }

    public static void onFlightTicks(EntityPlayer player, int ticks) {
        progress(player, AchievementKeys.FIRST_FLIGHT, ticks);
    }

    public static void onAnimalTamed(EntityPlayer player) {
        progress(player, AchievementKeys.ANIMAL_FRIEND, 1);
    }

    public static void onBlockMined(EntityPlayer player, int count) {
        progress(player, AchievementKeys.MINER_MASTER, count);
    }

    public static void onCropGrown(EntityPlayer player) {
        progress(player, AchievementKeys.TIME_MANAGER, 1);
    }

    public static void onMobAvoided(EntityPlayer player) {
        progress(player, AchievementKeys.INVISIBLE_MASTER, 1);
    }

    public static void onRobotWorkTicks(EntityPlayer player, int ticks) {
        progress(player, AchievementKeys.ROBOT_COMMANDER, ticks);
    }

    public static void onShrinkRoom(EntityPlayer player) {
        progress(player, AchievementKeys.SHRINK_ADVENTURER, 1);
    }

    public static void onDorayakiMade(EntityPlayer player, int count) {
        progress(player, AchievementKeys.DORAYAKI_LOVER, count);
    }

    public static void onAllResearchUnlocked(EntityPlayer player) {
        progress(player, AchievementKeys.OMNISCIENT, 1);
    }

    /**
     * Adds progress to an achievement counter and completes it when the threshold is reached.
     *
     * @param amount progress amount to add (must be positive to have any effect)
     */
    public static void progress(EntityPlayer player, AchievementKeys achievement, long amount) {
        if (player == null || player.world.isRemote) {
            return;
        }
        IDoraPlayerData data = DoraCapabilities.get(player);
        int granted = progress(data, achievement, amount);
        if (granted >= 0) {
            DoraUtil.sendMessage(player, TextFormatting.GOLD,
                    "★ 成就达成：" + translate(achievement) + "（+" + granted + " 研究点）");
        }
    }

    /**
     * Data-only achievement progress (server-authoritative, unit-testable). Adds progress and, when
     * the threshold is first reached, marks the achievement done and awards its point reward.
     *
     * @return points granted if the achievement was just completed, otherwise {@code -1}
     */
    public static int progress(IDoraPlayerData data, AchievementKeys achievement, long amount) {
        if (data == null || amount <= 0 || data.isAchievementDone(achievement.getId())) {
            return -1;
        }
        data.addCounter(achievement.getCounterKey(), amount);
        if (data.getCounter(achievement.getCounterKey()) >= achievement.getThreshold()) {
            data.markAchievementDone(achievement.getId());
            return data.addPoints(achievement.getPointReward());
        }
        return -1;
    }

    private static String translate(AchievementKeys achievement) {
        return "dorapack.achievement." + achievement.getId();
    }
}
