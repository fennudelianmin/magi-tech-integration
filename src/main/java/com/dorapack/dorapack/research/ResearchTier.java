package com.dorapack.dorapack.research;

/**
 * Research tiers with their associated item-key sets, used for prerequisite counting.
 */
public enum ResearchTier {

    NONE(new String[0]),

    TIER_1(new String[]{
            ResearchKeys.AIR_CANNON,
            ResearchKeys.BAG_EXPAND,
            ResearchKeys.TIME_WRAP,
            ResearchKeys.TRANSLATE_JELLY,
            ResearchKeys.MEMORY_BREAD,
            ResearchKeys.EMERGENCY_PILL,
            ResearchKeys.EXP_BOTTLE,
            ResearchKeys.MOMOTARO_FLUTE,
            ResearchKeys.MINER_DRILL,
            ResearchKeys.INVIS_CLOAK
    }),

    TIER_2(new String[]{
            ResearchKeys.GIANT_AIR_CANNON,
            ResearchKeys.ROCKET_BOOSTER,
            ResearchKeys.WEATHER_CONTROLLER,
            ResearchKeys.TIME_STOP_WATCH
    }),

    TIER_3(new String[]{
            ResearchKeys.MINING_ROBOT,
            ResearchKeys.CHARGING_STATION
    });

    private final String[] itemKeys;

    ResearchTier(String[] itemKeys) {
        this.itemKeys = itemKeys;
    }

    public String[] getItemKeys() {
        return itemKeys.clone();
    }
}
