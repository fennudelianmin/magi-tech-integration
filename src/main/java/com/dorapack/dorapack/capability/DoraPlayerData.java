package com.dorapack.dorapack.capability;

import com.dorapack.dorapack.config.DoraConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default {@link IDoraPlayerData} implementation backed by in-memory maps and serialized via NBT.
 */
public class DoraPlayerData implements IDoraPlayerData {

    private int totalPoints;
    private int pointsGainedToday;
    private long lastDay = -1L;

    private boolean basicTheory;
    private boolean advancedTheory;
    private boolean superTheory;

    private final Set<String> unlockedItems = new HashSet<>();
    private final Set<String> completedAchievements = new HashSet<>();
    private final Map<String, Integer> dailyUsage = new HashMap<>();
    private final Map<String, Long> counters = new HashMap<>();

    @Override
    public int getTotalPoints() {
        return totalPoints;
    }

    @Override
    public void setTotalPoints(int points) {
        this.totalPoints = Math.max(0, points);
    }

    @Override
    public int addPoints(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int cap = DoraConfig.dailyPointCap;
        int remaining = Math.max(0, cap - pointsGainedToday);
        int granted = Math.min(amount, remaining);
        totalPoints += granted;
        pointsGainedToday += granted;
        return granted;
    }

    @Override
    public int getPointsGainedToday() {
        return pointsGainedToday;
    }

    @Override
    public void refreshDay(long worldDay) {
        if (worldDay != lastDay) {
            lastDay = worldDay;
            pointsGainedToday = 0;
            dailyUsage.clear();
        }
    }

    @Override
    public boolean hasBasicTheory() {
        return basicTheory;
    }

    @Override
    public void setBasicTheory(boolean unlocked) {
        this.basicTheory = unlocked;
    }

    @Override
    public boolean hasAdvancedTheory() {
        return advancedTheory;
    }

    @Override
    public void setAdvancedTheory(boolean unlocked) {
        this.advancedTheory = unlocked;
    }

    @Override
    public boolean hasSuperTheory() {
        return superTheory;
    }

    @Override
    public void setSuperTheory(boolean unlocked) {
        this.superTheory = unlocked;
    }

    @Override
    public boolean isItemUnlocked(String itemKey) {
        return unlockedItems.contains(itemKey);
    }

    @Override
    public void unlockItem(String itemKey) {
        if (itemKey != null && !itemKey.isEmpty()) {
            unlockedItems.add(itemKey);
        }
    }

    @Override
    public Set<String> getUnlockedItems() {
        return unlockedItems;
    }

    @Override
    public int getDailyUsage(String key) {
        return dailyUsage.getOrDefault(key, 0);
    }

    @Override
    public void incrementDailyUsage(String key, int amount) {
        dailyUsage.merge(key, amount, Integer::sum);
    }

    @Override
    public long getCounter(String key) {
        return counters.getOrDefault(key, 0L);
    }

    @Override
    public void addCounter(String key, long amount) {
        counters.merge(key, amount, Long::sum);
    }

    @Override
    public boolean isAchievementDone(String key) {
        return completedAchievements.contains(key);
    }

    @Override
    public void markAchievementDone(String key) {
        if (key != null && !key.isEmpty()) {
            completedAchievements.add(key);
        }
    }

    @Override
    public Set<String> getCompletedAchievements() {
        return completedAchievements;
    }

    // --- Package-private accessors for the NBT storage class ---

    long getLastDay() {
        return lastDay;
    }

    void setLastDay(long lastDay) {
        this.lastDay = lastDay;
    }

    @Override
    public void setPointsGainedToday(int value) {
        this.pointsGainedToday = value;
    }

    Map<String, Integer> getDailyUsageMap() {
        return dailyUsage;
    }

    Map<String, Long> getCounterMap() {
        return counters;
    }
}
