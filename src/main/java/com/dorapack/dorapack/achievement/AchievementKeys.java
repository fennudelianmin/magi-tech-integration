package com.dorapack.dorapack.achievement;

/**
 * Achievement identifiers and their associated progress counter keys / thresholds / point rewards.
 *
 * <p>Kept as an enum so the manager can iterate all achievements generically when checking progress.</p>
 */
public enum AchievementKeys {

    FIRST_FLIGHT("first_flight", "flightTicks", 60 * 20, 15),
    DOOR_GOD("door_god", "doorTeleports", 100, 25),
    ANIMAL_FRIEND("animal_friend", "animalsTamed", 10, 20),
    MINER_MASTER("miner_master", "blocksMined", 1000, 30),
    TIME_MANAGER("time_manager", "cropsGrown", 100, 15),
    INVISIBLE_MASTER("invisible_master", "mobsAvoided", 100, 25),
    ROBOT_COMMANDER("robot_commander", "robotWorkTicks", 60 * 60 * 20, 40),
    OMNISCIENT("omniscient", "allResearch", 1, 100),
    SHRINK_ADVENTURER("shrink_adventurer", "shrinkRooms", 1, 10),
    DORAYAKI_LOVER("dorayaki_lover", "dorayakiMade", 10, 15);

    private final String id;
    private final String counterKey;
    private final long threshold;
    private final int pointReward;

    AchievementKeys(String id, String counterKey, long threshold, int pointReward) {
        this.id = id;
        this.counterKey = counterKey;
        this.threshold = threshold;
        this.pointReward = pointReward;
    }

    public String getId() {
        return id;
    }

    public String getCounterKey() {
        return counterKey;
    }

    public long getThreshold() {
        return threshold;
    }

    public int getPointReward() {
        return pointReward;
    }
}
