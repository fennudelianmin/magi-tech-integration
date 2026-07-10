package com.dorapack.dorapack;

import com.dorapack.dorapack.achievement.AchievementKeys;
import com.dorapack.dorapack.achievement.AchievementManager;
import com.dorapack.dorapack.capability.DoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the data-only achievement progress logic in {@link AchievementManager}: threshold
 * detection, one-shot completion, point rewards and daily-cap interaction.
 */
class AchievementManagerTest {

    private DoraPlayerData data;

    @BeforeEach
    void setUp() {
        DoraConfig.dailyPointCap = 100000;
        data = new DoraPlayerData();
    }

    @Test
    @DisplayName("progress below threshold accrues but does not complete")
    void progressBelowThreshold() {
        // SHRINK_ADVENTURER threshold is 1; use ANIMAL_FRIEND (threshold 10) for a partial case.
        int granted = AchievementManager.progress(data, AchievementKeys.ANIMAL_FRIEND, 5);
        assertEquals(-1, granted, "not yet complete");
        assertFalse(data.isAchievementDone(AchievementKeys.ANIMAL_FRIEND.getId()));
        assertEquals(5L, data.getCounter(AchievementKeys.ANIMAL_FRIEND.getCounterKey()));
    }

    @Test
    @DisplayName("reaching the threshold completes the achievement and grants its reward")
    void reachingThresholdCompletes() {
        AchievementManager.progress(data, AchievementKeys.ANIMAL_FRIEND, 5);
        int granted = AchievementManager.progress(data, AchievementKeys.ANIMAL_FRIEND, 5);
        assertEquals(AchievementKeys.ANIMAL_FRIEND.getPointReward(), granted);
        assertTrue(data.isAchievementDone(AchievementKeys.ANIMAL_FRIEND.getId()));
        assertEquals(AchievementKeys.ANIMAL_FRIEND.getPointReward(), data.getTotalPoints());
    }

    @Test
    @DisplayName("a single large increment can complete immediately")
    void singleIncrementCompletes() {
        int granted = AchievementManager.progress(data, AchievementKeys.MINER_MASTER, 5000);
        assertEquals(AchievementKeys.MINER_MASTER.getPointReward(), granted);
        assertTrue(data.isAchievementDone(AchievementKeys.MINER_MASTER.getId()));
    }

    @Test
    @DisplayName("achievements are one-shot: no double reward")
    void oneShotCompletion() {
        assertTrue(AchievementManager.progress(data, AchievementKeys.SHRINK_ADVENTURER, 1) >= 0);
        int pointsAfterFirst = data.getTotalPoints();

        int second = AchievementManager.progress(data, AchievementKeys.SHRINK_ADVENTURER, 1);
        assertEquals(-1, second, "already completed");
        assertEquals(pointsAfterFirst, data.getTotalPoints(), "no additional reward");
    }

    @Test
    @DisplayName("non-positive progress amounts are ignored")
    void nonPositiveIgnored() {
        assertEquals(-1, AchievementManager.progress(data, AchievementKeys.DOOR_GOD, 0));
        assertEquals(-1, AchievementManager.progress(data, AchievementKeys.DOOR_GOD, -3));
        assertEquals(0L, data.getCounter(AchievementKeys.DOOR_GOD.getCounterKey()));
    }

    @Test
    @DisplayName("null data is safe")
    void nullDataSafe() {
        assertEquals(-1, AchievementManager.progress((DoraPlayerData) null, AchievementKeys.DOOR_GOD, 1));
    }

    @Test
    @DisplayName("achievement reward respects the daily point cap")
    void rewardRespectsDailyCap() {
        DoraConfig.dailyPointCap = 5;
        data = new DoraPlayerData();
        // OMNISCIENT rewards 100, but only 5 can be granted today.
        int granted = AchievementManager.progress(data, AchievementKeys.OMNISCIENT, 1);
        assertEquals(5, granted);
        assertEquals(5, data.getTotalPoints());
        assertTrue(data.isAchievementDone(AchievementKeys.OMNISCIENT.getId()),
                "completion is still recorded even if reward was capped");
    }
}
