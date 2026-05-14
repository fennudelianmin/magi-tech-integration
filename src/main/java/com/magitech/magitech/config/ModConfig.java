package com.magitech.magitech.config;

import net.minecraftforge.common.config.Configuration;

public class ModConfig {

    public static Configuration config;

    // General
    public static String energyMode = "RF";
    public static double speedMultiplier = 1.0;
    public static boolean playParticles = true;

    // Livingrock Cultivator
    public static int cultivatorEnergyPerTick = 4;
    public static int cultivatorProcessTime = 1200;

    // Flower Crafting Table
    public static int flowerWaterPerCraft = 1000;
    public static int flowerTankCapacity = 4000;
    public static int flowerEnergyPerTick = 8;

    // Rune Altar
    public static int runeManaCapacity = 200000;
    public static int runeEnergyPerTick = 16;

    // Terra Plate
    public static int terraManaCapacity = 1000000;
    public static int terraManaCostPerCraft = 500000;
    public static int terraEnergyPerTick = 32;

    // Elven Trade
    public static int elvenManaCapacity = 50000;
    public static int elvenDefaultManaCost = 2000;
    public static int elvenEnergyPerTick = 8;

    public static void init(java.io.File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
        }
        loadConfig();
    }

    public static void loadConfig() {
        config.load();

        // General
        energyMode = config.getString("energy_mode", "general", "RF", "Energy mode: RF or AE", new String[]{"RF", "AE"});
        speedMultiplier = config.getFloat("speed_multiplier", "general", 1.0F, 0.1F, 10.0F, "Global speed multiplier");
        playParticles = config.getBoolean("play_particles", "general", true, "Show machine particles");

        // Livingrock Cultivator
        cultivatorEnergyPerTick = config.getInt("energy_per_tick", "machines.livingrock_cultivator", 4, 1, 1000, "FE per tick");
        cultivatorProcessTime = config.getInt("process_time_ticks", "machines.livingrock_cultivator", 1200, 20, 12000, "Process time in ticks");

        // Flower Crafting Table
        flowerWaterPerCraft = config.getInt("water_per_craft", "machines.flower_crafting_table", 1000, 100, 10000, "Water per craft (mB)");
        flowerTankCapacity = config.getInt("internal_water_tank", "machines.flower_crafting_table", 4000, 1000, 64000, "Water tank capacity (mB)");
        flowerEnergyPerTick = config.getInt("energy_per_tick", "machines.flower_crafting_table", 8, 1, 1000, "FE per tick");

        // Rune Altar
        runeManaCapacity = config.getInt("mana_capacity", "machines.rune_altar", 200000, 10000, 10000000, "Mana capacity");
        runeEnergyPerTick = config.getInt("energy_per_tick", "machines.rune_altar", 16, 1, 1000, "FE per tick");

        // Terra Plate
        terraManaCapacity = config.getInt("mana_capacity", "machines.terra_plate", 1000000, 100000, 100000000, "Mana capacity");
        terraManaCostPerCraft = config.getInt("mana_cost_per_craft", "machines.terra_plate", 500000, 10000, 100000000, "Mana cost per craft");
        terraEnergyPerTick = config.getInt("energy_per_tick", "machines.terra_plate", 32, 1, 1000, "FE per tick");

        // Elven Trade
        elvenManaCapacity = config.getInt("mana_capacity", "machines.elven_trade", 50000, 1000, 10000000, "Mana capacity");
        elvenDefaultManaCost = config.getInt("default_mana_cost", "machines.elven_trade", 2000, 10, 1000000, "Default mana cost per trade");
        elvenEnergyPerTick = config.getInt("energy_per_tick", "machines.elven_trade", 8, 1, 1000, "FE per tick");

        if (config.hasChanged()) {
            config.save();
        }
    }
}
