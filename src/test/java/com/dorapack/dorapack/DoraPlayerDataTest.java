package com.dorapack.dorapack;

import com.dorapack.dorapack.capability.DoraPlayerData;
import com.dorapack.dorapack.config.DoraConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DoraPlayerData}: research-point accrual with the daily cap, daily rollover,
 * usage counters and achievement/progress counters. Pure POJO logic, no Minecraft runtime required.
 */
class DoraPlayerDataTest {

    private DoraPlayerData data;

    @BeforeEach
    void setUp() {
        // Reset the config default the cap logic reads (tests never call DoraConfig.load).
        DoraConfig.dailyPointCap = 100;
        data = new DoraPlayerData();
    }

    @Test
    @DisplayName("addPoints grants the full amount below the daily cap")
    void addPointsBelowCap() {
        int granted = data.addPoints(30);
        assertEquals(30, granted);
        assertEquals(30, data.getTotalPoints());
        assertEquals(30, data.getPointsGainedToday());
    }

    @Test
    @DisplayName("addPoints is clamped by the remaining daily cap")
    void addPointsClampedByDailyCap() {
        assertEquals(80, data.addPoints(80));
        // Only 20 of the requested 50 remain under the 100/day cap.
        assertEquals(20, data.addPoints(50));
        assertEquals(100, data.getTotalPoints());
        assertEquals(100, data.getPointsGainedToday());
        // Nothing more can be gained today.
        assertEquals(0, data.addPoints(10));
        assertEquals(100, data.getTotalPoints());
    }

    @Test
    @DisplayName("non-positive point amounts are ignored")
    void addPointsNonPositiveIgnored() {
        assertEquals(0, data.addPoints(0));
        assertEquals(0, data.addPoints(-5));
        assertEquals(0, data.getTotalPoints());
    }

    @Test
    @DisplayName("refreshDay resets the daily counter only when the day changes")
    void refreshDayResetsDailyCounter() {
        data.refreshDay(1L);
        data.addPoints(100);
        assertEquals(0, data.addPoints(10), "cap reached, nothing granted");

        // Same day: counter must NOT reset.
        data.refreshDay(1L);
        assertEquals(0, data.addPoints(10));

        // New day: daily counter rolls over so points can accrue again.
        data.refreshDay(2L);
        assertEquals(50, data.addPoints(50));
        assertEquals(150, data.getTotalPoints());
    }

    @Test
    @DisplayName("refreshDay clears daily usage but preserves total points")
    void refreshDayClearsDailyUsage() {
        data.refreshDay(5L);
        data.incrementDailyUsage("door", 3);
        data.addPoints(40);
        assertEquals(3, data.getDailyUsage("door"));

        data.refreshDay(6L);
        assertEquals(0, data.getDailyUsage("door"), "usage resets on new day");
        assertEquals(40, data.getTotalPoints(), "total points persist across days");
    }

    @Test
    @DisplayName("setTotalPoints floors at zero")
    void setTotalPointsFloorsAtZero() {
        data.setTotalPoints(-20);
        assertEquals(0, data.getTotalPoints());
    }

    @Test
    @DisplayName("daily usage accumulates via merge")
    void dailyUsageAccumulates() {
        assertEquals(0, data.getDailyUsage("teleport"));
        data.incrementDailyUsage("teleport", 1);
        data.incrementDailyUsage("teleport", 2);
        assertEquals(3, data.getDailyUsage("teleport"));
    }

    @Test
    @DisplayName("counters and achievement completion behave independently")
    void countersAndAchievements() {
        data.addCounter("blocks", 500L);
        data.addCounter("blocks", 500L);
        assertEquals(1000L, data.getCounter("blocks"));

        assertFalse(data.isAchievementDone("miner_master"));
        data.markAchievementDone("miner_master");
        assertTrue(data.isAchievementDone("miner_master"));
        assertTrue(data.getCompletedAchievements().contains("miner_master"));
    }

    @Test
    @DisplayName("item unlock set rejects null/empty keys")
    void unlockItemRejectsBlankKeys() {
        data.unlockItem(null);
        data.unlockItem("");
        assertTrue(data.getUnlockedItems().isEmpty());
        data.unlockItem("air_cannon");
        assertTrue(data.isItemUnlocked("air_cannon"));
    }
}
