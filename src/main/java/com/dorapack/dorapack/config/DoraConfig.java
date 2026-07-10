package com.dorapack.dorapack.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Server-adjustable balance configuration, persisted to {@code config/dorapack/balance.cfg}.
 *
 * <p>All gameplay magic numbers from the design document live here so operators can tune them
 * without recompiling. Values are loaded once during pre-init and cached in plain fields for
 * hot-path access (no per-tick config lookups).</p>
 */
public final class DoraConfig {

    private static final String CATEGORY_ITEMS = "items";
    private static final String CATEGORY_RESEARCH = "research";
    private static final String CATEGORY_STRUCTURES = "structures";
    private static final String CATEGORY_ROBOT = "robot";

    private static Configuration config;

    // --- Item balance ---
    public static int bambooCopterFlightSeconds = 10;
    public static int bambooCopterDurability = 50;
    public static int anywhereDoorCooldownSeconds = 20;
    public static int anywhereDoorMaxDistance = 1000;
    public static int miniAirCannonDurability = 30;
    public static int miniAirCannonCooldownSeconds = 8;
    public static int bagInitialSlots = 8;
    public static int bagMaxSlots = 32;
    public static int bagExpandCostBase = 50;
    public static int tameFoodDailyLimitPerAnimal = 1;
    public static int headlampSpeedPenaltyPercent = 3;

    // Tier 1
    public static int airCannonDurability = 100;
    public static int airCannonCooldownSeconds = 3;
    public static int timeWrapDurability = 20;
    public static int timeWrapDailyLimit = 10;
    public static int memoryBreadDailyLimit = 3;
    public static int emergencyPillDailyLimit = 1;
    public static int expBottleCapacity = 30;
    public static int invisCloakDurability = 100;
    public static int invisCloakDurationSeconds = 10;

    // Tier 2
    public static int giantCannonMaxRange = 10;
    public static int giantCannonMaxChargeSeconds = 3;
    public static int rocketBoosterCooldownSeconds = 15;
    public static int rocketBoosterDistance = 15;
    public static int weatherControllerDailyLimit = 1;
    public static int timeStopSeconds = 5;
    public static int timeStopRadius = 15;
    public static int timeStopCooldownMinutes = 15;

    // --- Research ---
    public static int dailyPointCap = 100;
    public static int pointsCraftingTable = 10;
    public static int pointsEnterCave = 20;
    public static int pointsKillZombies = 15;
    public static int killZombiesRequired = 10;
    public static int basicTheoryCost = 10;
    public static int advancedTheoryCost = 50;
    public static int superTheoryCost = 100;

    // --- Structures ---
    public static int shrinkTunnelDurationSeconds = 600;
    public static int shrinkTunnelSpeedBonusPercent = 20;
    public static int doorHubDailyCrossDimLimit = 5;
    public static int doorHubCrossDimCost = 10;

    // --- Robot ---
    public static int robotInitialEnergy = 1000;
    public static int robotInitialStorageSlots = 16;
    public static int chargingStationCapacity = 100_000;
    public static int chargingStationRate = 100;

    private DoraConfig() {
    }

    public static void load(File configDir) {
        File file = new File(new File(configDir, "dorapack"), "balance.cfg");
        config = new Configuration(file);
        sync();
    }

    public static void sync() {
        if (config == null) {
            return;
        }
        loadItems();
        loadResearch();
        loadStructures();
        loadRobot();
        if (config.hasChanged()) {
            config.save();
        }
    }

    private static void loadItems() {
        bambooCopterFlightSeconds = getInt(CATEGORY_ITEMS, "bambooCopterFlightSeconds", bambooCopterFlightSeconds, 1, 600);
        bambooCopterDurability = getInt(CATEGORY_ITEMS, "bambooCopterDurability", bambooCopterDurability, 1, 10000);
        anywhereDoorCooldownSeconds = getInt(CATEGORY_ITEMS, "anywhereDoorCooldownSeconds", anywhereDoorCooldownSeconds, 0, 3600);
        anywhereDoorMaxDistance = getInt(CATEGORY_ITEMS, "anywhereDoorMaxDistance", anywhereDoorMaxDistance, 1, 100000);
        miniAirCannonDurability = getInt(CATEGORY_ITEMS, "miniAirCannonDurability", miniAirCannonDurability, 1, 10000);
        miniAirCannonCooldownSeconds = getInt(CATEGORY_ITEMS, "miniAirCannonCooldownSeconds", miniAirCannonCooldownSeconds, 0, 3600);
        bagInitialSlots = getInt(CATEGORY_ITEMS, "bagInitialSlots", bagInitialSlots, 1, 54);
        bagMaxSlots = getInt(CATEGORY_ITEMS, "bagMaxSlots", bagMaxSlots, 1, 54);
        bagExpandCostBase = getInt(CATEGORY_ITEMS, "bagExpandCostBase", bagExpandCostBase, 1, 10000);
        tameFoodDailyLimitPerAnimal = getInt(CATEGORY_ITEMS, "tameFoodDailyLimitPerAnimal", tameFoodDailyLimitPerAnimal, 1, 100);
        headlampSpeedPenaltyPercent = getInt(CATEGORY_ITEMS, "headlampSpeedPenaltyPercent", headlampSpeedPenaltyPercent, 0, 100);

        airCannonDurability = getInt(CATEGORY_ITEMS, "airCannonDurability", airCannonDurability, 1, 10000);
        airCannonCooldownSeconds = getInt(CATEGORY_ITEMS, "airCannonCooldownSeconds", airCannonCooldownSeconds, 0, 3600);
        timeWrapDurability = getInt(CATEGORY_ITEMS, "timeWrapDurability", timeWrapDurability, 1, 10000);
        timeWrapDailyLimit = getInt(CATEGORY_ITEMS, "timeWrapDailyLimit", timeWrapDailyLimit, 1, 1000);
        memoryBreadDailyLimit = getInt(CATEGORY_ITEMS, "memoryBreadDailyLimit", memoryBreadDailyLimit, 1, 1000);
        emergencyPillDailyLimit = getInt(CATEGORY_ITEMS, "emergencyPillDailyLimit", emergencyPillDailyLimit, 1, 1000);
        expBottleCapacity = getInt(CATEGORY_ITEMS, "expBottleCapacity", expBottleCapacity, 1, 10000);
        invisCloakDurability = getInt(CATEGORY_ITEMS, "invisCloakDurability", invisCloakDurability, 1, 10000);
        invisCloakDurationSeconds = getInt(CATEGORY_ITEMS, "invisCloakDurationSeconds", invisCloakDurationSeconds, 1, 600);

        giantCannonMaxRange = getInt(CATEGORY_ITEMS, "giantCannonMaxRange", giantCannonMaxRange, 1, 64);
        giantCannonMaxChargeSeconds = getInt(CATEGORY_ITEMS, "giantCannonMaxChargeSeconds", giantCannonMaxChargeSeconds, 1, 60);
        rocketBoosterCooldownSeconds = getInt(CATEGORY_ITEMS, "rocketBoosterCooldownSeconds", rocketBoosterCooldownSeconds, 0, 3600);
        rocketBoosterDistance = getInt(CATEGORY_ITEMS, "rocketBoosterDistance", rocketBoosterDistance, 1, 128);
        weatherControllerDailyLimit = getInt(CATEGORY_ITEMS, "weatherControllerDailyLimit", weatherControllerDailyLimit, 1, 100);
        timeStopSeconds = getInt(CATEGORY_ITEMS, "timeStopSeconds", timeStopSeconds, 1, 60);
        timeStopRadius = getInt(CATEGORY_ITEMS, "timeStopRadius", timeStopRadius, 1, 64);
        timeStopCooldownMinutes = getInt(CATEGORY_ITEMS, "timeStopCooldownMinutes", timeStopCooldownMinutes, 0, 1440);
    }

    private static void loadResearch() {
        dailyPointCap = getInt(CATEGORY_RESEARCH, "dailyPointCap", dailyPointCap, 1, 100000);
        pointsCraftingTable = getInt(CATEGORY_RESEARCH, "pointsCraftingTable", pointsCraftingTable, 0, 100000);
        pointsEnterCave = getInt(CATEGORY_RESEARCH, "pointsEnterCave", pointsEnterCave, 0, 100000);
        pointsKillZombies = getInt(CATEGORY_RESEARCH, "pointsKillZombies", pointsKillZombies, 0, 100000);
        killZombiesRequired = getInt(CATEGORY_RESEARCH, "killZombiesRequired", killZombiesRequired, 1, 100000);
        basicTheoryCost = getInt(CATEGORY_RESEARCH, "basicTheoryCost", basicTheoryCost, 0, 100000);
        advancedTheoryCost = getInt(CATEGORY_RESEARCH, "advancedTheoryCost", advancedTheoryCost, 0, 100000);
        superTheoryCost = getInt(CATEGORY_RESEARCH, "superTheoryCost", superTheoryCost, 0, 100000);
    }

    private static void loadStructures() {
        shrinkTunnelDurationSeconds = getInt(CATEGORY_STRUCTURES, "shrinkTunnelDurationSeconds", shrinkTunnelDurationSeconds, 1, 36000);
        shrinkTunnelSpeedBonusPercent = getInt(CATEGORY_STRUCTURES, "shrinkTunnelSpeedBonusPercent", shrinkTunnelSpeedBonusPercent, 0, 100);
        doorHubDailyCrossDimLimit = getInt(CATEGORY_STRUCTURES, "doorHubDailyCrossDimLimit", doorHubDailyCrossDimLimit, 0, 1000);
        doorHubCrossDimCost = getInt(CATEGORY_STRUCTURES, "doorHubCrossDimCost", doorHubCrossDimCost, 0, 10000);
    }

    private static void loadRobot() {
        robotInitialEnergy = getInt(CATEGORY_ROBOT, "robotInitialEnergy", robotInitialEnergy, 1, 10000000);
        robotInitialStorageSlots = getInt(CATEGORY_ROBOT, "robotInitialStorageSlots", robotInitialStorageSlots, 1, 54);
        chargingStationCapacity = getInt(CATEGORY_ROBOT, "chargingStationCapacity", chargingStationCapacity, 1, 100000000);
        chargingStationRate = getInt(CATEGORY_ROBOT, "chargingStationRate", chargingStationRate, 1, 1000000);
    }

    private static int getInt(String category, String key, int defaultValue, int min, int max) {
        return config.getInt(key, category, defaultValue, min, max, "");
    }

    public static Configuration getRawConfig() {
        return config;
    }
}
