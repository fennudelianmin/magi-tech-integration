package com.dorapack.dorapack.research;

/**
 * String keys identifying research-locked items and theory tiers.
 *
 * <p>These are stable identifiers used both in the capability unlock set and in the research GUI,
 * decoupled from the item registry names so refactors don't invalidate saved data.</p>
 */
public final class ResearchKeys {

    // Theory tiers
    public static final String BASIC_THEORY = "basic_theory";
    public static final String ADVANCED_THEORY = "advanced_theory";
    public static final String SUPER_THEORY = "super_theory";

    // Tier 1 unlockable items
    public static final String AIR_CANNON = "air_cannon";
    public static final String BAG_EXPAND = "bag_expand";
    public static final String TIME_WRAP = "time_wrap";
    public static final String TRANSLATE_JELLY = "translate_jelly";
    public static final String MEMORY_BREAD = "memory_bread";
    public static final String EMERGENCY_PILL = "emergency_pill";
    public static final String EXP_BOTTLE = "exp_bottle";
    public static final String MOMOTARO_FLUTE = "momotaro_flute";
    public static final String MINER_DRILL = "miner_drill";
    public static final String INVIS_CLOAK = "invis_cloak";

    // Tier 2 unlockable items
    public static final String GIANT_AIR_CANNON = "giant_air_cannon";
    public static final String ROCKET_BOOSTER = "rocket_booster";
    public static final String WEATHER_CONTROLLER = "weather_controller";
    public static final String TIME_STOP_WATCH = "time_stop_watch";

    // Tier 3 unlockable items
    public static final String MINING_ROBOT = "mining_robot";
    public static final String CHARGING_STATION = "charging_station";

    private ResearchKeys() {
    }
}
