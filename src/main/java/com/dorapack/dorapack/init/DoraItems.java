package com.dorapack.dorapack.init;

import net.minecraft.item.Item;

/**
 * Central registry-name constants and static holders for every DoraPack item.
 *
 * <p>Fields are populated during the {@code RegistryEvent.Register<Item>} phase by
 * {@link com.dorapack.dorapack.handler.RegistryHandler}. Names are the single source of truth for
 * registry names, translation keys and model paths.</p>
 */
public final class DoraItems {

    // --- No-unlock item names ---
    public static final String NAME_BAMBOO_COPTER = "bamboo_copter";
    public static final String NAME_ANYWHERE_DOOR_ITEM = "anywhere_door";
    public static final String NAME_MINI_AIR_CANNON = "mini_air_cannon";
    public static final String NAME_BAG_INITIAL = "bag_initial";
    public static final String NAME_TAME_FOOD = "tame_food";
    public static final String NAME_HEADLAMP = "headlamp";

    // --- Tier 1 ---
    public static final String NAME_AIR_CANNON = "air_cannon";
    public static final String NAME_TIME_WRAP = "time_wrap";
    public static final String NAME_TRANSLATE_JELLY = "translate_jelly";
    public static final String NAME_MEMORY_BREAD = "memory_bread";
    public static final String NAME_EMERGENCY_PILL = "emergency_pill";
    public static final String NAME_EXP_BOTTLE = "exp_bottle";
    public static final String NAME_MOMOTARO_FLUTE = "momotaro_flute";
    public static final String NAME_MINER_DRILL = "miner_drill";
    public static final String NAME_INVIS_CLOAK = "invis_cloak";
    public static final String NAME_BAG_UPGRADE = "bag_upgrade";

    // --- Tier 2 ---
    public static final String NAME_GIANT_AIR_CANNON = "giant_air_cannon";
    public static final String NAME_ROCKET_BOOSTER = "rocket_booster";
    public static final String NAME_WEATHER_CONTROLLER = "weather_controller";
    public static final String NAME_TIME_STOP_WATCH = "time_stop_watch";

    // --- Creative ultimate ---
    public static final String NAME_BECOME_DORAEMON = "become_doraemon";
    public static final String NAME_WISDOM_KING = "wisdom_king";

    // --- Tier 3 ---
    public static final String NAME_MINING_ROBOT = "mining_robot";
    public static final String NAME_ROBOT_UPGRADE = "robot_upgrade";

    // --- Four-dimensional crafting materials ---
    public static final String NAME_DIMENSION_SHARD = "dimension_shard";
    public static final String NAME_DIMENSION_CORE = "dimension_core";

    // --- Static item holders (populated at registration time) ---
    public static Item BAMBOO_COPTER;
    public static Item ANYWHERE_DOOR_ITEM;
    public static Item MINI_AIR_CANNON;
    public static Item BAG_INITIAL;
    public static Item TAME_FOOD;
    public static Item HEADLAMP;

    public static Item AIR_CANNON;
    public static Item TIME_WRAP;
    public static Item TRANSLATE_JELLY;
    public static Item MEMORY_BREAD;
    public static Item EMERGENCY_PILL;
    public static Item EXP_BOTTLE;
    public static Item MOMOTARO_FLUTE;
    public static Item MINER_DRILL;
    public static Item INVIS_CLOAK;
    public static Item BAG_UPGRADE;

    public static Item GIANT_AIR_CANNON;
    public static Item ROCKET_BOOSTER;
    public static Item WEATHER_CONTROLLER;
    public static Item TIME_STOP_WATCH;

    public static Item BECOME_DORAEMON;
    public static Item WISDOM_KING;

    public static Item MINING_ROBOT;
    public static Item ROBOT_UPGRADE;

    public static Item DIMENSION_SHARD;
    public static Item DIMENSION_CORE;

    private DoraItems() {
    }
}
