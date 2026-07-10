package com.dorapack.dorapack.capability;

import java.util.Set;

/**
 * Per-player persistent data: research progress, daily-limited usages and achievement counters.
 *
 * <p>Stored via a Forge Capability attached to every {@code EntityPlayer}. All mutation happens
 * server-side; the client receives a synced snapshot for GUI display.</p>
 */
public interface IDoraPlayerData {

    // --- Research points ---

    int getTotalPoints();

    void setTotalPoints(int points);

    /**
     * Adds points respecting the configured daily cap.
     *
     * @param amount raw amount to add
     * @return the amount actually granted after the daily cap is applied
     */
    int addPoints(int amount);

    int getPointsGainedToday();

    /** Overwrites the "points gained today" counter; used by the server-to-client sync snapshot. */
    void setPointsGainedToday(int value);

    /** Rolls the daily counters over if the world day changed. */
    void refreshDay(long worldDay);

    // --- Research tree ---

    boolean hasBasicTheory();

    void setBasicTheory(boolean unlocked);

    boolean hasAdvancedTheory();

    void setAdvancedTheory(boolean unlocked);

    boolean hasSuperTheory();

    void setSuperTheory(boolean unlocked);

    boolean isItemUnlocked(String itemKey);

    void unlockItem(String itemKey);

    Set<String> getUnlockedItems();

    // --- Daily-limited usage ---

    int getDailyUsage(String key);

    void incrementDailyUsage(String key, int amount);

    // --- Achievement / progress counters ---

    long getCounter(String key);

    void addCounter(String key, long amount);

    boolean isAchievementDone(String key);

    void markAchievementDone(String key);

    Set<String> getCompletedAchievements();
}
